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
    val showSport by viewModel.showSport.collectAsState()
    val showSavoir by viewModel.showSavoir.collectAsState()
    val showPoem by viewModel.showPoem.collectAsState()
    val showNews by viewModel.showNews.collectAsState()

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
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState), // 🟢 Scroll activé
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "⚙️ Paramètres",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- SECTION 1 : AFFICHAGE ---
            SettingsSectionTitle("Affichage de l'accueil")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ToggleRow("🏋️‍♂️ Sport", showSport) { viewModel.toggleSport(it) }
                    ToggleRow("🧠 Culture G", showSavoir) { viewModel.toggleSavoir(it) }
                    ToggleRow("📜 Poésie", showPoem) { viewModel.togglePoem(it) }
                    ToggleRow("📰 Actualités", showNews) { viewModel.toggleNews(it) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECTION 2 : OUTILS & CONTENU ---
            SettingsSectionTitle("Outils & Contenu")
            
            // BOUTON ÉQUIPEMENT
            SettingsActionCard(
                icon = "🛠️",
                title = "Mon Équipement",
                subtitle = "Gérer le matériel de base pour l'IA",
                onClick = onEquipmentClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            // BOUTON POÉSIE
            SettingsActionCard(
                icon = "📜",
                title = "Journal de Poésie",
                subtitle = "Relire vos poèmes quotidiens",
                onClick = onPoemClick,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECTION 3 : INTELLIGENCE ARTIFICIELLE ---
            SettingsSectionTitle("Configuration IA & News")
            
            SettingsActionCard(
                icon = "🔄",
                title = "Régénérer le programme",
                subtitle = "Personnaliser et recalculer via l'IA",
                onClick = { montrePopUpConfirmation = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsActionCard(
                icon = "🔑",
                title = "Clé API Gemini",
                subtitle = "Modifier la clé utilisée par l'IA",
                onClick = { montrePopUpApiKey = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsActionCard(
                icon = "🗞️",
                title = "Clé News API",
                subtitle = "Modifier la clé pour les actualités",
                onClick = { montrePopUpNewsApiKey = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECTION 4 : SYSTÈME ---
            SettingsSectionTitle("Système")
            
            SettingsActionCard(
                icon = if (isCheckingUpdate) "⏳" else "☁️",
                title = "Vérifier les mises à jour",
                subtitle = "Vérifier s'il y a des changements sur GitHub",
                onClick = {
                    scope.launch {
                        isCheckingUpdate = true
                        val sha = updateManager.checkForUpdates()
                        if (sha != null) {
                            updateManager.downloadAndInstallApk()
                        }
                        isCheckingUpdate = false
                    }
                },
                enabled = !isCheckingUpdate
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun SettingsActionCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}