package com.example.myapplication.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DailyWorkoutSection(
    uiState: WorkoutUiState,
    jourSelectionne: LocalDate,
    aujourdhui: LocalDate,
    onStartWorkoutClick: () -> Unit
) {
    // On conserve ta logique exacte pour le titre de la séance
    val estAujourdhui = jourSelectionne.isEqual(aujourdhui)
    Column {
        Spacer(modifier = Modifier.height(8.dp))

        when (val state = uiState) {
            is WorkoutUiState.Idle, is WorkoutUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is WorkoutUiState.Error -> {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(text = state.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            is WorkoutUiState.Success -> {
                val programme = state.program

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onStartWorkoutClick() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (state.isCompleted) {
                            Text(
                                text = programme.sport,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(15.dp))
                            Text(
                                "✅ Séance terminée ! Beau travail !",
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "💪 ${programme.sport}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Durée estimée : ${programme.duree_minutes} min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (programme.exercices.isEmpty()) {
                                Text(
                                    "😴 Pas d'exercices prévus. Repos bien mérité !",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                // Conservation de tes règles d'affichage personnalisées (Mouvements vs Secondes/Minutes)
                                programme.exercices.forEach { exo ->
                                    if (!exo.en_duree) {
                                        Text(
                                            text = "• ${exo.nom} (${exo.series} x ${exo.repetitions})",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    } else {
                                        Text(
                                            text = "• ${exo.nom} pour ${if (exo.series > 1) "${exo.series} x " else ""} ${if (exo.repetitions >= 60) "${exo.repetitions / 60}${if (exo.repetitions % 60 != 0) ":${exo.repetitions % 60}" else ""} minutes" else "${exo.repetitions} secondes"}",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }

                            if (programme.exercices.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                if (estAujourdhui) {
                                    Text(
                                        "👉 Appuyez pour commencer",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}