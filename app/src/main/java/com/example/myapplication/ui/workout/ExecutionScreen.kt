package com.example.myapplication.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ExecutionScreen(viewModel: WorkoutViewModel, onBackClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    var indexExerciceActuel by remember { mutableStateOf(0) }
    var serieActuelle by remember { mutableStateOf(1) }
    var seanceTerminee by remember { mutableStateOf(false) }
    var seanceSauvegarde by remember { mutableStateOf(false) }

    // Gestion du chrono de récupération (Repos)
    var tempsRestant by remember { mutableStateOf(0) }
    var chronoActif by remember { mutableStateOf(false) }

    // Gestion du chrono d'effort (Pour les exercices en_duree)
    var tempsExerciceRestant by remember { mutableStateOf(0) }
    var exerciceChronoActif by remember { mutableStateOf(false) }

    // Helper pour formater les secondes en MM:SS
    fun formatChrono(secondes: Int): String {
        val m = secondes / 60
        val s = secondes % 60
        return String.format("%02d:%02d", m, s)
    }

    // Initialisation automatique du temps d'effort à chaque nouvelle série / exercice
    LaunchedEffect(indexExerciceActuel, serieActuelle, chronoActif) {
        if (!chronoActif && !seanceTerminee) {
            val exercices = (uiState as? WorkoutUiState.Success)?.program?.exercices ?: emptyList()
            if (exercices.isNotEmpty() && indexExerciceActuel < exercices.size) {
                val exo = exercices[indexExerciceActuel]
                if (exo.en_duree) {
                    tempsExerciceRestant = exo.repetitions
                    exerciceChronoActif = true
                }
            }
        }
    }

    // 1. Boucle du compte à rebours de Récupération (Repos)
    LaunchedEffect(chronoActif) {
        while (chronoActif && tempsRestant > 0) {
            delay(1000L)
            if (chronoActif) {
                tempsRestant -= 1
                if (tempsRestant == 0) {
                    chronoActif = false
                }
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        when (val state = uiState) {
            is WorkoutUiState.Loading, is WorkoutUiState.Idle -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            is WorkoutUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Erreur : ${state.message}", color = MaterialTheme.colorScheme.error) }
            }
            is WorkoutUiState.Success -> {
                val exercices = state.program.exercices
                Text(text = if(seanceTerminee || state.isCompleted) "Séance Terminée" else "🏋️‍♂️ Séance en cours", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(24.dp))

                // 🟢 FONCTION MUTUALISÉE : Gère la transition d'exercice ou de repos (Partagée par le Timer et le nouveau Bouton)
                val passerALaSuite = {
                    exerciceChronoActif = false
                    tempsExerciceRestant = 0
                    if (indexExerciceActuel < exercices.size) {
                        val exoActuel = exercices[indexExerciceActuel]
                        if (serieActuelle < exoActuel.series) {
                            serieActuelle++
                            tempsRestant = exoActuel.recup_secondes
                            chronoActif = true
                        } else {
                            if (indexExerciceActuel < exercices.size - 1) {
                                indexExerciceActuel++
                                serieActuelle = 1
                                tempsRestant = exoActuel.recup_secondes
                                chronoActif = true
                            } else {
                                seanceTerminee = true
                            }
                        }
                    }
                }

                // 2. Boucle du compte à rebours d'Effort (Gainage, Course...)
                LaunchedEffect(exerciceChronoActif) {
                    while (exerciceChronoActif && tempsExerciceRestant > 0 && !chronoActif) {
                        delay(1000L)
                        if (exerciceChronoActif && !chronoActif) {
                            tempsExerciceRestant -= 1
                            if (tempsExerciceRestant == 0) {
                                passerALaSuite() // Appel de la fonction de transition propre
                            }
                        }
                    }
                }

                // Sauvegarde automatique dans Room dès que la séance passe à "terminée"
                LaunchedEffect(seanceTerminee) {
                    if (seanceTerminee and !seanceSauvegarde) {
                        viewModel.markWorkoutAsCompleted(state.date)
                        seanceSauvegarde = true

                    }
                }

                if (exercices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("😴", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Aujourd'hui c'est repos !", textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = {
                                if (!seanceTerminee) {
                                    seanceTerminee = true
                                } else{
                                    onBackClick()
                                }
                            }) {
                                if(!seanceTerminee){
                                    Text("Valider")
                                } else{
                                    Text("Quitter")
                                }
                            }
                        }
                    }
                } else if (seanceTerminee || state.isCompleted) {
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))) {
                        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FÉLICITATIONS !", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("✅", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Séance complétée avec succès. Ton corps te remercie Gabriel !", textAlign = TextAlign.Center, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onBackClick , modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)) { Text("Terminer et quitter", fontWeight = FontWeight.Bold) }
                } else {
                    val exoActuel = exercices[indexExerciceActuel]

                    LinearProgressIndicator(progress = { (indexExerciceActuel.toFloat() / exercices.size) }, modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Exercice ${indexExerciceActuel + 1} sur ${exercices.size}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = exoActuel.nom, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(12.dp))

                            if (exoActuel.en_duree) {
                                Text(
                                    text = formatChrono(tempsExerciceRestant),
                                    fontSize = 54.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (exoActuel.series > 1) "Objectif : ${exoActuel.series} blocs de temps" else "Effort continu",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "${exoActuel.repetitions} Répétitions",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            if (exoActuel.series != 1) {
                                Text(
                                    text = "Série : $serieActuelle / ${exoActuel.series}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (exoActuel.conseil.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "💡 Conseil : ${exoActuel.conseil}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Fenêtre de récupération (Repos)
                    AnimatedVisibility(visible = chronoActif || tempsRestant > 0) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (tempsRestant > 0 && !chronoActif) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = if (chronoActif) "⏱ Récupération..." else "🔔 Repos terminé !", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Text(text = if (tempsRestant > 0) "00:${String.format("%02d", tempsRestant)}" else "Prêt !", fontSize = 54.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 🟢 NOUVEAU : Bouton secondaire pour couper prématurément le chrono d'effort
                    // Il apparaît uniquement si l'exercice se compte en temps, que le repos n'est pas actif et qu'il reste du temps.
                    if (exoActuel.en_duree && !chronoActif && tempsExerciceRestant > 0) {
                        OutlinedButton(
                            onClick = { passerALaSuite() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(bottom = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("⏭ Terminer l'effort maintenant", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }

                    // Bouton d'action principal
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        onClick = {
                            if (chronoActif) {
                                tempsRestant = 0
                                chronoActif = false
                            } else {
                                if (exoActuel.en_duree) {
                                    exerciceChronoActif = !exerciceChronoActif
                                } else {
                                    if (serieActuelle < exoActuel.series) {
                                        serieActuelle++
                                        tempsRestant = exoActuel.recup_secondes
                                        chronoActif = true
                                    } else {
                                        if (indexExerciceActuel < exercices.size - 1) {
                                            indexExerciceActuel++
                                            serieActuelle = 1
                                            tempsRestant = exoActuel.recup_secondes
                                            chronoActif = true
                                        } else {
                                            seanceTerminee = true
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Text(
                            text = when {
                                chronoActif -> "Sauter le repos ⏭"
                                exoActuel.en_duree -> if (exerciceChronoActif) "Pause ⏸" else "Reprendre ▶"
                                serieActuelle < exoActuel.series -> "Valider la Série $serieActuelle ✅"
                                indexExerciceActuel < exercices.size - 1 -> "Passer à l'exercice suivant ⏭"
                                else -> "Terminer la séance 🎉"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Quitter l'entraînement", color = Color.Gray) }
                }
            }
        }
    }
}