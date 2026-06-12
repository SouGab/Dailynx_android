package com.example.myapplication.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.AppBottomNavigationBar // 🟢 Import de la barre

@Composable
fun EquipmentScreen(
    viewModel: WorkoutViewModel,
    onHomeClick: () -> Unit,          // 🟢 Nouvelle action de navigation
    onHistoryClick: () -> Unit        // 🟢 Nouvelle action de navigation
) {
    val equipmentList by viewModel.equipmentList.collectAsState()
    var nouveauMateriel by remember { mutableStateOf("") }
    var estModifie by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadEquipmentList()
    }

    Scaffold(
        bottomBar = {
            // 🟢 Ajout de la barre avec l'onglet Settings allumé
            AppBottomNavigationBar(
                isSettingsSelected = true,
                onHistoryClick = onHistoryClick,
                onHomeClick = onHomeClick,
                onSettingsClick = { /* Déjà dessus */ }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // En-tête
            Text(
                text = "🛠️ Mon Équipement",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "L'IA adaptera vos séances selon cette liste.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Formulaire d'ajout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = nouveauMateriel,
                    onValueChange = { nouveauMateriel = it },
                    label = { Text("Ex: Haltères, Barre, Élastique...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (nouveauMateriel.isNotBlank()) {
                            viewModel.addEquipment(nouveauMateriel)
                            nouveauMateriel = ""
                            estModifie = true
                        }
                    },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Ajouter", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Liste du matériel
            Text("Mes outils actuels :", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            if (equipmentList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Aucun matériel enregistré.\nSéances au poids du corps uniquement. 🧍", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = equipmentList,
                        key = { it.id }
                    ) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "💪  ${item.name}", fontSize = 18.sp, fontWeight = FontWeight.Medium)

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            viewModel.deleteEquipment(item)
                                            estModifie = true
                                        }
                                        .padding(8.dp)
                                ) {
                                    Text("🗑️", fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Bouton "Annuler" dynamique
            if (estModifie) {
                OutlinedButton(
                    onClick = {
                        viewModel.cancelEquipmentModifications()
                        estModifie = false
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                ) {
                    Text("❌ Annuler les modifications", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}