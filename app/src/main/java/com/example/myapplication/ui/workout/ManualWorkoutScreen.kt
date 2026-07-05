package com.example.myapplication.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.Exercise
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualWorkoutScreen(
    viewModel: WorkoutViewModel,
    date: String,
    onBackClick: () -> Unit
) {
    var sportName by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    
    // Liste d'exercices locale
    val exercises = remember { mutableStateListOf<Exercise>() }
    
    // Équipements
    val equipmentList by viewModel.equipmentList.collectAsState()
    val equipmentSelection = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        viewModel.loadEquipmentList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajouter une séance ($date)") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) { Text("Retour") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Informations générales
            Text("Informations générales", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sportName,
                onValueChange = { sportName = it },
                label = { Text("Nom du sport (ex: Musculation)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it },
                label = { Text("Durée (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Équipements utilisés
            Text("Équipements utilisés", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            if (equipmentList.isEmpty()) {
                Text("Aucun équipement enregistré.", color = Color.Gray, fontSize = 14.sp)
            } else {
                equipmentList.forEach { equipment ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            equipmentSelection[equipment.name] = !(equipmentSelection[equipment.name] ?: false)
                        }
                    ) {
                        Checkbox(
                            checked = equipmentSelection[equipment.name] ?: false,
                            onCheckedChange = { equipmentSelection[equipment.name] = it }
                        )
                        Text(equipment.name)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section Exercices
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Exercices", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Button(onClick = {
                    exercises.add(Exercise(nom = "", en_duree = false, series = 3, repetitions = 10, recup_secondes = 60, conseil = ""))
                }) {
                    Text("+ Ajouter")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            exercises.forEachIndexed { index, exercise ->
                ExerciseInputRow(
                    exercise = exercise,
                    onUpdate = { updated -> exercises[index] = updated },
                    onDelete = { exercises.removeAt(index) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val selectedEquip = equipmentSelection.filter { it.value }.keys.joinToString(", ")
                    val finalExercises = if (selectedEquip.isNotEmpty()) {
                        exercises.map { it.copy(conseil = (if (it.conseil.isBlank()) "" else it.conseil + " | ") + "Matériel: $selectedEquip") }
                    } else exercises

                    viewModel.saveManualWorkout(
                        date = date,
                        sport = sportName,
                        duration = duration.toIntOrNull() ?: 0,
                        exercises = finalExercises
                    )
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = sportName.isNotBlank() && exercises.isNotEmpty()
            ) {
                Text("Enregistrer la séance", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ExerciseInputRow(
    exercise: Exercise,
    onUpdate: (Exercise) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = exercise.nom,
                    onValueChange = { onUpdate(exercise.copy(nom = it)) },
                    label = { Text("Nom de l'exercice") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Text("🗑️", fontSize = 20.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = exercise.series.toString(),
                    onValueChange = { onUpdate(exercise.copy(series = it.toIntOrNull() ?: 0)) },
                    label = { Text("Séries") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = exercise.repetitions.toString(),
                    onValueChange = { onUpdate(exercise.copy(repetitions = it.toIntOrNull() ?: 0)) },
                    label = { Text(if (exercise.en_duree) "Sec" else "Rép") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = exercise.en_duree,
                    onCheckedChange = { onUpdate(exercise.copy(en_duree = it)) }
                )
                Text("Exercice chronométré ?", fontSize = 14.sp)
            }
        }
    }
}
