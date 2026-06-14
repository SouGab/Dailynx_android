package com.example.myapplication.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.AppBottomNavigationBar
import com.example.myapplication.ui.components.ApiKeyDialog
import com.example.myapplication.data.network.UpdateManager
import java.time.LocalDate
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: WorkoutViewModel,
    updateManager: UpdateManager,
    onHomeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onEquipmentClick: () -> Unit,
    onPoemClick: () -> Unit
) {
    val equipmentList by viewModel.equipmentList.collectAsState()
    var montrePopUpConfirmation by remember { mutableStateOf(false) }
    var montrePopUpApiKey by remember { mutableStateOf(false) }
    var montrePopUpNewsApiKey by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }

    // --- ÉTATS LOCAUX POUR LA PERSONNALISATION DE L'IA ---
    var genererSport by remember { mutableStateOf(true) }
    var genererInfo by remember { mutableStateOf(true) }
    var contraintesText by remember { mutableStateOf("") }
    var regenererSemaine by remember { mutableStateOf(false) }

    // Map pour suivre l'activation à la volée de chaque matériel
    val equipementsSelectionnes = remember { mutableStateMapOf<String, Boolean>() }


    // Charger les équipements au démarrage et initialiser la sélection à "true"
    LaunchedEffect(Unit) {
        viewModel.loadEquipmentList()
    }

    // Synchroniser la map locale quand la liste de la BDD change
    LaunchedEffect(equipmentList) {
        equipmentList.forEach { equipment ->
            if (!equipementsSelectionnes.containsKey(equipment.name)) {
                equipementsSelectionnes[equipment.name] = true
            }
        }
    }

    if (montrePopUpConfirmation) {
        AlertDialog(
            onDismissRequest = { montrePopUpConfirmation = false },
            title = { Text("Configuration de l'IA", fontWeight = FontWeight.Bold) },
            text = {
                // Zone scrollable au cas où tu as beaucoup de matériel enregistré
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Choisissez les sections à recalculer :",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = genererSport, onCheckedChange = { genererSport = it })
                        Text("🏋️‍♂️ Entraînements (Sport)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = genererInfo, onCheckedChange = { genererInfo = it })
                        Text("🧠 Culture générale (Savoir)")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- SÉLECTEUR DE MATÉRIEL TEMPORAIRE ---
                    Text(
                        text = "Matériel disponible pour cette séance :",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (equipmentList.isEmpty()) {
                        Text("Aucun outil enregistré. (Poids du corps par défaut)", fontSize = 13.sp, color = Color.DarkGray)
                    } else {
                        equipmentList.forEach { equipment ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = equipementsSelectionnes[equipment.name] ?: true,
                                    onCheckedChange = { equipementsSelectionnes[equipment.name] = it },
                                    modifier = Modifier.scale(0.8f) // Légèrement plus petit pour l'intégration
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(equipment.name, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- ZONE TEXTE DES CONTRAINTES ---
                    Text(
                        text = "Contraintes ou douleurs particulières ?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contraintesText,
                        onValueChange = { contraintesText = it },
                        placeholder = { Text("Ex: douleur genou droit, fatigue...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Régénérer la semaine",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Bold
                        )

                        Switch(
                            checked = regenererSemaine,
                            onCheckedChange = { regenererSemaine = it },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = genererSport || genererInfo, // Désactivé si rien n'est coché
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    onClick = {
                        montrePopUpConfirmation = false

                        // Extraire la liste textuelle des outils uniquement cochés
                        val listeMaterielFiltre = equipementsSelectionnes
                            .filter { it.value }
                            .keys
                            .toList()

                        // Envoi de l'ensemble des configurations personnalisées au ViewModel
                        viewModel.forceCustomRegenerateRemainingWeek(
                            contraintesPerso = contraintesText.trim(),
                            dateSelectionneeString = LocalDate.now().toString(),
                            includeSport = genererSport,
                            includeInfo = genererInfo,
                            materielCoche = listeMaterielFiltre,
                            regenererSemaine = regenererSemaine
                        )

                        onHomeClick() // Retour à l'accueil
                    }
                ) { Text("Lancer la génération", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { montrePopUpConfirmation = false }) { Text("Annuler") }
            }
        )
    }

    if (montrePopUpApiKey) {
        ApiKeyDialog(
            onDismiss = { montrePopUpApiKey = false },
            onSave = { viewModel.saveApiKey(it) }
        )
    }

    if (montrePopUpNewsApiKey) {
        ApiKeyDialog(
            title = "Clé NewsAPI.org",
            description = "Entrez votre clé NewsAPI pour les actualités.",
            onDismiss = { montrePopUpNewsApiKey = false },
            onSave = { viewModel.saveNewsApiKey(it) }
        )
    }

    Scaffold(
        bottomBar = {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "⚙️ Paramètres",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(32.dp))

            // --- BOUTON 1 : ÉQUIPEMENT ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEquipmentClick() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🛠️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Mon Équipement", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Gérer le matériel de base pour l'IA", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- BOUTON 1.5 : POÉSIE ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPoemClick() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📜", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Journal de Poésie", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Relire vos poèmes quotidiens", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- BOUTON 2 : RÉGÉNÉRER LE PROGRAMME ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { montrePopUpConfirmation = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔄", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Régénérer le programme", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Personnaliser et recalculer via l'IA", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- BOUTON 3 : CLÉ API ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { montrePopUpApiKey = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔑", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Clé API Gemini", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Modifier la clé utilisée par l'IA", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- BOUTON 3.5 : CLÉ NEWS API ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { montrePopUpNewsApiKey = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🗞️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Clé News API", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Modifier la clé pour les actualités", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- BOUTON 4 : MISE À JOUR ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isCheckingUpdate) {
                        scope.launch {
                            isCheckingUpdate = true
                            val sha = updateManager.checkForUpdates()
                            if (sha != null) {
                                // Déclenche le téléchargement et l'installation
                                updateManager.downloadAndInstallApk()
                                // Note: Le SHA sera mis à jour après l'install réussie via MainActivity au prochain reboot
                                // ou on pourrait le faire ici si on est sûr.
                            }
                            isCheckingUpdate = false
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isCheckingUpdate) "⏳" else "☁️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Vérifier les mises à jour", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Vérifier s'il y a des changements sur GitHub", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}