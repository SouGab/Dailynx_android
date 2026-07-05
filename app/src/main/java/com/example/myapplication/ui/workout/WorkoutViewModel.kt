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
import com.example.myapplication.data.model.Poem
import com.example.myapplication.data.model.PoemEntity
import com.example.myapplication.data.model.Article
import com.example.myapplication.data.model.NewsEntity
import com.example.myapplication.data.prompt.Prompts
import com.example.myapplication.data.network.GeminiService
import com.example.myapplication.data.network.ApiKeyManager
import com.example.myapplication.data.network.PoemService
import com.example.myapplication.data.network.NewsService

sealed interface WorkoutUiState {
    object Idle : WorkoutUiState
    object Loading : WorkoutUiState
    data class Success(val program: WorkoutProgram, val date: String, val isCompleted: Boolean) : WorkoutUiState
    data class Error(val message: String) : WorkoutUiState
}

sealed interface PoemUiState {
    object Idle : PoemUiState
    object Loading : PoemUiState
    data class Success(val poem: Poem) : PoemUiState
    data class Error(val message: String) : PoemUiState
}

sealed interface NewsUiState {
    object Idle : NewsUiState
    object Loading : NewsUiState
    data class Success(val articles: List<Article>) : NewsUiState
    data class Error(val message: String) : NewsUiState
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

    private val apiKeyManager = ApiKeyManager(application)
    private var geminiService: GeminiService? = null
    private var newsService: NewsService? = null
    private val poemService = PoemService()

    init {
        apiKeyManager.getApiKey()?.let {
            geminiService = GeminiService(it)
        }
        apiKeyManager.getNewsApiKey()?.let {
            newsService = NewsService(it)
        }
    }

    fun hasApiKey(): Boolean = apiKeyManager.hasApiKey()
    fun hasNewsApiKey(): Boolean = apiKeyManager.hasNewsApiKey()

    fun saveApiKey(key: String) {
        apiKeyManager.setApiKey(key)
        geminiService = GeminiService(key)
    }

    fun saveNewsApiKey(key: String) {
        apiKeyManager.setNewsApiKey(key)
        newsService = NewsService(key)
    }

    private val database = AppDatabase.getDatabase(application)
    private val workoutDao = database.workoutDao()
    private val equipmentDao = database.equipmentDao()
    private val learningDao = database.learningDao()
    private val poemDao = database.poemDao()
    private val newsDao = database.newsDao()

    private val _uiState = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Idle)
    val uiState: StateFlow<WorkoutUiState> = _uiState

    private val _completedDates = MutableStateFlow<Set<String>>(emptySet())
    val completedDates: StateFlow<Set<String>> = _completedDates

    private val _equipmentList = MutableStateFlow<List<EquipmentEntity>>(emptyList())
    val equipmentList: StateFlow<List<EquipmentEntity>> = _equipmentList

    private val _learningState = MutableStateFlow<LearningUiState>(LearningUiState.Idle)
    val learningState: StateFlow<LearningUiState> = _learningState

    private val _poemState = MutableStateFlow<PoemUiState>(PoemUiState.Idle)
    val poemState: StateFlow<PoemUiState> = _poemState

    private val _newsState = MutableStateFlow<NewsUiState>(NewsUiState.Idle)
    val newsState: StateFlow<NewsUiState> = _newsState

    private val _showSport = MutableStateFlow(apiKeyManager.isSportEnabled())
    val showSport: StateFlow<Boolean> = _showSport

    private val _showSavoir = MutableStateFlow(apiKeyManager.isSavoirEnabled())
    val showSavoir: StateFlow<Boolean> = _showSavoir

    private val _showPoem = MutableStateFlow(apiKeyManager.isPoemEnabled())
    val showPoem: StateFlow<Boolean> = _showPoem

    private val _showNews = MutableStateFlow(apiKeyManager.isNewsEnabled())
    val showNews: StateFlow<Boolean> = _showNews

    fun toggleSport(enabled: Boolean) {
        apiKeyManager.setSportEnabled(enabled)
        _showSport.value = enabled
    }

    fun toggleSavoir(enabled: Boolean) {
        apiKeyManager.setSavoirEnabled(enabled)
        _showSavoir.value = enabled
    }

    fun togglePoem(enabled: Boolean) {
        apiKeyManager.setPoemEnabled(enabled)
        _showPoem.value = enabled
    }

    fun toggleNews(enabled: Boolean) {
        apiKeyManager.setNewsEnabled(enabled)
        _showNews.value = enabled
    }

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

    fun saveManualWorkout(date: String, sport: String, duration: Int, exercises: List<Exercise>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val program = WorkoutProgram(sport, duration, exercises)
                val json = Json.encodeToString(program)
                val entity = WorkoutEntity(
                    date = date,
                    sport = sport,
                    dureeMinutes = duration,
                    exercicesJson = json,
                    isCompleted = true
                )
                workoutDao.insertWorkout(entity)
                _completedDates.value = _completedDates.value + date

                // Rafraîchir l'état si on est sur cette date
                val currentState = _uiState.value
                if (currentState is WorkoutUiState.Success && currentState.date == date) {
                    _uiState.value = WorkoutUiState.Success(program, date, true)
                } else if (currentState is WorkoutUiState.Idle || currentState is WorkoutUiState.Error) {
                     _uiState.value = WorkoutUiState.Success(program, date, true)
                }
            }
        }
    }

    // --- CHARGEMENT SIMULTANÉ DE LA SÉANCE ET DU APPRENTISSAGE ---
    fun loadOrCreateDailyProgram(contraintes: String, dateString: String = getTodayDateString(), sport: String? = null) {
        viewModelScope.launch {
            _uiState.value = WorkoutUiState.Loading
            _learningState.value = LearningUiState.Loading
            _poemState.value = PoemUiState.Loading
            _newsState.value = NewsUiState.Loading

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

                // 3. Chargement Poème
                val localPoem = poemDao.getPoemByDate(dateString)
                if (localPoem != null) {
                    try {
                        val lines = Json.decodeFromString<List<String>>(localPoem.linesJson)
                        val poem = Poem(title = localPoem.title, author = localPoem.author, lines = lines, linecount = lines.size.toString())
                        _poemState.value = PoemUiState.Success(poem)
                    } catch (e: Exception) {
                        _poemState.value = PoemUiState.Error("Erreur de lecture du poème.")
                    }
                } else {
                    // Si on a pas de poème local, on ne fait rien ici pour éviter les requêtes à chaque clic.
                    // Ils seront générés via la logique de semaine.
                    _poemState.value = PoemUiState.Idle
                }

                // 4. Chargement News
                val localNews = newsDao.getNewsByDate(dateString)
                if (localNews.isNotEmpty()) {
                    val articles = localNews.map {
                        Article(title = it.title, description = it.description, url = it.url, source = com.example.myapplication.data.model.Source(it.sourceName ?: ""))
                    }
                    _newsState.value = NewsUiState.Success(articles)
                } else {
                    generateWeeklyNewsFromNetwork(dateString)
                }
            }
        }
    }

    private suspend fun generateWeeklyNewsFromNetwork(dateSelectionneeString: String) {
        val response = newsService?.fetchFrenchNews()
        if (response != null && response.articles.isNotEmpty()) {
            val newsEntities = response.articles.map {
                NewsEntity(
                    date = dateSelectionneeString,
                    title = it.title,
                    description = it.description,
                    url = it.url,
                    sourceName = it.source?.name
                )
            }
            newsDao.insertNews(newsEntities)
            _newsState.value = NewsUiState.Success(response.articles)
        } else {
            _newsState.value = NewsUiState.Error("Pas de news disponibles.")
        }
    }

    private suspend fun generateWeeklyPoemsFromNetwork(dateSelectionneeString: String) {
        val dateCible = LocalDate.parse(dateSelectionneeString)
        val lundiDeCetteSemaine = dateCible.with(DayOfWeek.MONDAY)

        val poems = poemService.fetchRandomPoems(7)

        if (poems.isNotEmpty()) {
            poems.forEachIndexed { index, poem ->
                if (index < 7) {
                    val dateExacteString = lundiDeCetteSemaine.plusDays(index.toLong()).toString()
                    val entity = PoemEntity(
                        date = dateExacteString,
                        title = poem.title,
                        author = poem.author,
                        linesJson = Json.encodeToString(poem.lines)
                    )
                    poemDao.insertPoem(entity)
                }
            }

            val updatedLocal = poemDao.getPoemByDate(dateSelectionneeString)
            if (updatedLocal != null) {
                val lines = Json.decodeFromString<List<String>>(updatedLocal.linesJson)
                val poem = Poem(title = updatedLocal.title, author = updatedLocal.author, lines = lines, linecount = lines.size.toString())
                _poemState.value = PoemUiState.Success(poem)
            }
        } else {
            _poemState.value = PoemUiState.Error("Impossible de récupérer les poèmes.")
        }
    }

    private suspend fun generateFullWeekFromNetwork(contraintes: String, dateSelectionneeString: String, forceFutureOnly: Boolean = false, sport: String? = null) {
        val aujourdhui = LocalDate.now()
        val dateCible = LocalDate.parse(dateSelectionneeString)
        val lundiDeCetteSemaine = dateCible.with(DayOfWeek.MONDAY)

        // On génère aussi les poèmes pour la semaine si on régénère le sport
        generateWeeklyPoemsFromNetwork(dateSelectionneeString)
        // On génère aussi les news pour la semaine
        generateWeeklyNewsFromNetwork(dateSelectionneeString)

        val equipementsUtilisateur = equipmentDao.getAllEquipment()
        val texteEquipement = if (equipementsUtilisateur.isEmpty()) {
            "Pas de matériel disponible (faire des exercices au poids du corps uniquement)."
        } else {
            "Utilise UNIQUEMENT les équipements suivants pour concevoir les exercices : " + equipementsUtilisateur.joinToString { it.name }
        }

        val contraintesGlobales = "$contraintes. Matériel disponible : $texteEquipement"

        val prompt = Prompts.getWeeklyWorkoutPrompt(contraintesGlobales, sport)
        val jsonResult = geminiService?.fetchWorkoutJson(prompt)

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
        val jsonResult = geminiService?.fetchWorkoutJson(prompt)

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
                    val jsonResult = geminiService?.fetchWorkoutJson(prompt)

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