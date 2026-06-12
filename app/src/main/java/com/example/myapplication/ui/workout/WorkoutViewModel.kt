package com.example.myapplication.ui.workout

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.model.WorkoutEntity
import com.example.myapplication.data.model.WorkoutProgram
import com.example.myapplication.data.model.Exercise
import com.example.myapplication.data.model.EquipmentEntity
import com.example.myapplication.data.model.LearningEntity
import com.example.myapplication.data.model.Info
import com.example.myapplication.data.model.WeeklyLearningResponse
import com.example.myapplication.data.prompt.Prompts
import com.example.myapplication.data.network.GeminiService

sealed interface WorkoutUiState {
    object Idle : WorkoutUiState
    object Loading : WorkoutUiState
    data class Success(val program: WorkoutProgram, val date: String, val isCompleted: Boolean) : WorkoutUiState
    data class Error(val message: String) : WorkoutUiState
}

@Serializable
data class DailyWorkoutInput(
    val jour: String,
    val sport: String,
    val duree_minutes: Int,
    val exercices: List<Exercise>
)

@Serializable
data class WeeklyWorkoutResponse(
    val jours: List<DailyWorkoutInput>
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiService = GeminiService()
    private val database = AppDatabase.getDatabase(application)
    private val workoutDao = database.workoutDao()
    private val equipmentDao = database.equipmentDao()
    private val learningDao = database.learningDao()

    private val _uiState = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Idle)
    val uiState: StateFlow<WorkoutUiState> = _uiState

    private val _completedDates = MutableStateFlow<Set<String>>(emptySet())
    val completedDates: StateFlow<Set<String>> = _completedDates

    private val _equipmentList = MutableStateFlow<List<EquipmentEntity>>(emptyList())
    val equipmentList: StateFlow<List<EquipmentEntity>> = _equipmentList

    private val _learningState = MutableStateFlow<LearningUiState>(LearningUiState.Idle)
    val learningState: StateFlow<LearningUiState> = _learningState

    private val _monthlyWorkouts = MutableStateFlow<List<WorkoutEntity>>(emptyList())
    val monthlyWorkouts: StateFlow<List<WorkoutEntity>> = _monthlyWorkouts

    private val _monthlyLearnings = MutableStateFlow<List<LearningEntity>>(emptyList())
    val monthlyLearnings: StateFlow<List<LearningEntity>> = _monthlyLearnings

    private var originalEquipmentSnapshot: List<EquipmentEntity> = emptyList()

    fun getTodayDateString(): String {
        return LocalDate.now().toString()
    }

    // --- HISTORIQUE MENSUEL ---
    fun loadMonthData(yearMonthString: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val startDate = "$yearMonthString-01"
                val endDate = "$yearMonthString-31"
                _monthlyWorkouts.value = workoutDao.getWorkoutsInRange(startDate, endDate)
                _monthlyLearnings.value = learningDao.getLearningsLikedInRange(startDate, endDate)
            }
        }
    }

    // --- GESTION DU MATÉRIEL ---
    fun loadEquipmentList() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val list = equipmentDao.getAllEquipment()
                _equipmentList.value = list
                originalEquipmentSnapshot = list
            }
        }
    }

    fun addEquipment(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                equipmentDao.insertEquipment(EquipmentEntity(name = name.trim()))
                _equipmentList.value = equipmentDao.getAllEquipment()
            }
        }
    }

    fun deleteEquipment(equipment: EquipmentEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                equipmentDao.deleteEquipment(equipment)
                _equipmentList.value = equipmentDao.getAllEquipment()
            }
        }
    }

    fun cancelEquipmentModifications() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val currentList = equipmentDao.getAllEquipment()
                currentList.forEach { equipmentDao.deleteEquipment(it) }
                originalEquipmentSnapshot.forEach { equipmentDao.insertEquipment(EquipmentEntity(name = it.name)) }
                _equipmentList.value = equipmentDao.getAllEquipment()
            }
        }
    }

    // --- LOGIQUE DU LIKE (FAVORIS) ---
    fun toggleLikeLearning(dateString: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val local = learningDao.getLearningByDate(dateString)
                if (local != null) {
                    val nouveauStatusLike = !local.isLiked

                    // 1. Sauvegarde SQL
                    learningDao.updateLikedStatus(dateString, nouveauStatusLike)

                    // 2. Refresh instantané de l'état UI
                    val currentState = _learningState.value
                    if (currentState is LearningUiState.Success) {
                        _learningState.value = currentState.copy(isLiked = nouveauStatusLike)
                    }
                }
            }
        }
    }

    // --- COMPLETION ENTRAÎNEMENT ---
    fun markWorkoutAsCompleted(dateString: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Log.i("Complete", dateString)
                workoutDao.updateCompletionStatus(dateString, true)
            }
            _completedDates.value = _completedDates.value + dateString

            val currentState = _uiState.value
            if (currentState is WorkoutUiState.Success && currentState.date == dateString) {
                _uiState.value = currentState.copy(isCompleted = true)
            }
        }
    }

    // --- CHARGEMENT SIMULTANÉ DE LA SÉANCE ET DU APPRENTISSAGE ---
    fun loadOrCreateDailyProgram(contraintes: String, dateString: String = getTodayDateString(), sport: String? = null) {
        viewModelScope.launch {
            _uiState.value = WorkoutUiState.Loading
            _learningState.value = LearningUiState.Loading

            withContext(Dispatchers.IO) {
                val dateCible = LocalDate.parse(dateString)
                val lundi = dateCible.with(DayOfWeek.MONDAY).toString()
                val dimanche = dateCible.with(DayOfWeek.SUNDAY).toString()
                _completedDates.value = workoutDao.getCompletedDatesInRange(lundi, dimanche).toSet()

                // 1. Chargement Entraînement
                val localWorkout = workoutDao.getWorkoutByDate(dateString)
                if (localWorkout != null) {
                    try {
                        val program = Json.decodeFromString<WorkoutProgram>(localWorkout.exercicesJson)
                        _uiState.value = WorkoutUiState.Success(program, dateString, localWorkout.isCompleted)
                    } catch (e: Exception) {
                        _uiState.value = WorkoutUiState.Error("Erreur de lecture locale du sport.")
                    }
                } else {
                    generateFullWeekFromNetwork(contraintes, dateString, sport = sport)
                }

                // 2. Chargement Savoir (avec récupération de l'état isLiked)
                val localLearning = learningDao.getLearningByDate(dateString)
                if (localLearning != null) {
                    try {
                        val infoObj = Json.decodeFromString<Info>(localLearning.infoJson)
                        _learningState.value = LearningUiState.Success(infoObj, localLearning.isLiked)
                    } catch (e: Exception) {
                        _learningState.value = LearningUiState.Error("Erreur de lecture locale du savoir.")
                    }
                } else {
                    generateWeeklyLearningFromNetwork(dateString)
                }
            }
        }
    }

    private suspend fun generateFullWeekFromNetwork(contraintes: String, dateSelectionneeString: String, forceFutureOnly: Boolean = false, sport: String? = null) {
        val aujourdhui = LocalDate.now()
        val dateCible = LocalDate.parse(dateSelectionneeString)
        val lundiDeCetteSemaine = dateCible.with(DayOfWeek.MONDAY)

        val equipementsUtilisateur = equipmentDao.getAllEquipment()
        val texteEquipement = if (equipementsUtilisateur.isEmpty()) {
            "Pas de matériel disponible (faire des exercices au poids du corps uniquement)."
        } else {
            "Utilise UNIQUEMENT les équipements suivants pour concevoir les exercices : " + equipementsUtilisateur.joinToString { it.name }
        }

        val contraintesGlobales = "$contraintes. Matériel disponible : $texteEquipement"

        val prompt = Prompts.getWeeklyWorkoutPrompt(contraintesGlobales, sport)
        val jsonResult = geminiService.fetchWorkoutJson(prompt)

        if (jsonResult != null) {
            try {
                val weeklyResponse = Json.decodeFromString<WeeklyWorkoutResponse>(jsonResult)

                weeklyResponse.jours.forEachIndexed { index, dailyInput ->
                    val dateExacte = lundiDeCetteSemaine.plusDays(index.toLong())
                    val dateExacteString = dateExacte.toString()

                    if (forceFutureOnly && dateExacte.isBefore(aujourdhui)) {
                        return@forEachIndexed
                    }

                    val dailyProgram = WorkoutProgram(sport = dailyInput.sport, duree_minutes = dailyInput.duree_minutes, exercices = dailyInput.exercices)
                    val singleDayJson = Json.encodeToString(dailyProgram)

                    val entity = WorkoutEntity(
                        date = dateExacteString,
                        sport = dailyInput.sport,
                        dureeMinutes = dailyInput.duree_minutes,
                        exercicesJson = singleDayJson,
                        isCompleted = false
                    )
                    workoutDao.insertWorkout(entity)
                }

                val updatedLocal = workoutDao.getWorkoutByDate(dateSelectionneeString)
                if (updatedLocal != null) {
                    val program = Json.decodeFromString<WorkoutProgram>(updatedLocal.exercicesJson)
                    _uiState.value = WorkoutUiState.Success(program, dateSelectionneeString, updatedLocal.isCompleted)
                }
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error("Erreur de répartition du sport.")
            }
        } else {
            _uiState.value = WorkoutUiState.Error("IA indisponible pour le sport.")
        }
    }

    private suspend fun generateWeeklyLearningFromNetwork(dateSelectionneeString: String) {
        val dateCible = LocalDate.parse(dateSelectionneeString)
        val lundiDeCetteSemaine = dateCible.with(DayOfWeek.MONDAY)

        val prompt = Prompts.getWeeklyLearningPrompt()
        val jsonResult = geminiService.fetchWorkoutJson(prompt)

        if (jsonResult != null) {
            try {
                val learningResponse = Json.decodeFromString<WeeklyLearningResponse>(jsonResult)

                learningResponse.lecons.forEachIndexed { index, dailyInput ->
                    val dateExacteString = lundiDeCetteSemaine.plusDays(index.toLong()).toString()
                    val infoJsonText = Json.encodeToString(dailyInput.info)

                    learningDao.insertLearning(
                        LearningEntity(date = dateExacteString, infoJson = infoJsonText, isLiked = false)
                    )
                }

                val updatedLocal = learningDao.getLearningByDate(dateSelectionneeString)
                if (updatedLocal != null) {
                    val infoObj = Json.decodeFromString<Info>(updatedLocal.infoJson)
                    _learningState.value = LearningUiState.Success(infoObj, updatedLocal.isLiked)
                }
            } catch (e: Exception) {
                _learningState.value = LearningUiState.Error("Erreur de répartition des leçons.")
            }
        } else {
            _learningState.value = LearningUiState.Error("IA indisponible pour le savoir.")
        }
    }

    // 🟢 À AJOUTER DANS TON WORKOUTVIEWMODEL.KT

    /**
     * Force la régénération de la semaine en appliquant les filtres d'équipements et de contraintes textuelles à la volée.
     */
    fun forceCustomRegenerateRemainingWeek(
        contraintesPerso: String,
        dateSelectionneeString: String,
        includeSport: Boolean,
        includeInfo: Boolean,
        materielCoche: List<String>,
        regenererSemaine: Boolean
    ) {
        viewModelScope.launch {
            if (includeSport) _uiState.value = WorkoutUiState.Loading
            if (includeInfo) _learningState.value = LearningUiState.Loading

            withContext(Dispatchers.IO) {
                // 1. Si le sport est coché, on re-génère avec la liste d'outils filtrée par la pop-up
                if (includeSport) {
                    val aujourdhui = LocalDate.now()
                    val dateCible = LocalDate.parse(dateSelectionneeString)
                    val lundiDeCetteSemaine = dateCible.with(DayOfWeek.MONDAY)

                    // Construction précise de la règle d'équipement pour l'IA
                    val texteEquipement = if (materielCoche.isEmpty()) {
                        "Pas de matériel disponible (faire des exercices au poids du corps uniquement)."
                    } else {
                        "Utilise UNIQUEMENT les équipements suivants pour concevoir les exercices : " + materielCoche.joinToString { it }
                    }

                    val contraintesGlobales = if (contraintesPerso.isBlank()) {
                        texteEquipement
                    } else {
                        "$contraintesPerso. Matériel disponible : $texteEquipement"
                    }

                    val prompt = Prompts.getWeeklyWorkoutPrompt(contraintesGlobales, null)
                    val jsonResult = geminiService.fetchWorkoutJson(prompt)

                    if (jsonResult != null) {
                        try {
                            val weeklyResponse = Json.decodeFromString<WeeklyWorkoutResponse>(jsonResult)
                            weeklyResponse.jours.forEachIndexed { index, dailyInput ->
                                val dateExacte = lundiDeCetteSemaine.plusDays(index.toLong())
                                if (dateExacte.isBefore(aujourdhui) && !regenererSemaine) return@forEachIndexed

                                val dailyProgram = WorkoutProgram(sport = dailyInput.sport, duree_minutes = dailyInput.duree_minutes, exercices = dailyInput.exercices)
                                val singleDayJson = Json.encodeToString(dailyProgram)

                                workoutDao.insertWorkout(
                                    WorkoutEntity(
                                        date = dateExacte.toString(),
                                        sport = dailyInput.sport,
                                        dureeMinutes = dailyInput.duree_minutes,
                                        exercicesJson = singleDayJson,
                                        isCompleted = false
                                    )
                                )
                            }

                            // Rafraîchir la vue d'accueil sur la date cible
                            val updatedLocal = workoutDao.getWorkoutByDate(dateSelectionneeString)
                            if (updatedLocal != null) {
                                val program = Json.decodeFromString<WorkoutProgram>(updatedLocal.exercicesJson)
                                _uiState.value = WorkoutUiState.Success(program, dateSelectionneeString, updatedLocal.isCompleted)
                            }
                        } catch (e: Exception) {
                            _uiState.value = WorkoutUiState.Error("Erreur lors du traitement de la réponse sport.")
                        }
                    } else {
                        _uiState.value = WorkoutUiState.Error("L'IA n'a pas répondu pour le sport.")
                    }
                }

                // 2. Si la culture générale est cochée, on appelle sa propre branche réseau
                if (includeInfo) {
                    generateWeeklyLearningFromNetwork(dateSelectionneeString)
                }
            }
        }
    }
}