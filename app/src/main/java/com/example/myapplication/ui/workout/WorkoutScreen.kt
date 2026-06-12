package com.example.myapplication.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WorkoutScreen(viewModel: WorkoutViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = {
            viewModel.loadOrCreateDailyProgram("Pas de matériel, douleur poignet")
        }) {
            Text("Générer ma séance du jour")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gestion de l'affichage selon l'état du ViewModel
        when (val state = uiState) {
            is WorkoutUiState.Idle -> Text("Cliquez sur le bouton pour générer votre entraînement.")
            is WorkoutUiState.Loading -> CircularProgressIndicator() // Un spinner de chargement
            is WorkoutUiState.Error -> Text("Erreur : ${state.message}", color = MaterialTheme.colorScheme.error)
            is WorkoutUiState.Success -> {
                Text(text = "${state.program.sport} - ${state.program.duree_minutes} min", style = MaterialTheme.typography.headlineMedium)

                LazyColumn {
                    items(state.program.exercices) { exo ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(exo.nom, style = MaterialTheme.typography.titleMedium)
                                Text("${exo.series}x${exo.repetitions} - Repos: ${exo.recup_secondes}s")
                                Text("Conseil: ${exo.conseil}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}