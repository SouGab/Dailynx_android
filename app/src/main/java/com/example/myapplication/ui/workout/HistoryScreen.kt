package com.example.myapplication.ui.workout

import android.util.Log
import androidx.compose.animation.AnimatedVisibility // 🟢 Ajouté pour une transition fluide
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.myapplication.data.model.Info
import com.example.myapplication.data.model.WorkoutProgram
import com.example.myapplication.ui.components.AppBottomNavigationBar
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: WorkoutViewModel,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val monthlyWorkouts by viewModel.monthlyWorkouts.collectAsState()
    val monthlyLearnings by viewModel.monthlyLearnings.collectAsState()

    var moisSelectionne by remember { mutableStateOf(YearMonth.now()) }
    val aujourdhui = remember { LocalDate.now() }
    val scrollState = rememberScrollState()

    // 🟢 ÉTATS LOCAUX POUR GÉRER L'OUVERTURE DES DROPDOWNS
    var sportsExpanded by remember { mutableStateOf(false) }
    var themesExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(moisSelectionne) {
        viewModel.loadMonthData(moisSelectionne.toString())
    }

    val statsSports = remember(monthlyWorkouts) {
        val map = mutableMapOf<String, Int>()
        monthlyWorkouts.filter { it.isCompleted }.forEach {
            map[it.sport] = (map[it.sport] ?: 0) + 1
        }
        map.toList().sortedByDescending { it.second }
    }

    val statsThemes = remember(monthlyLearnings) {
        val map = mutableMapOf<String, String>()
        monthlyLearnings.forEach { entity ->
            try {
                val info = Json.decodeFromString<Info>(entity.infoJson)
                Log.i("INFO", info.toString())
                map[info.nom] = info.sujet
            } catch (e: Exception) { }
        }
        map.toList().sortedByDescending { it.second }
    }
    Log.i("HELP", statsThemes.toString())

    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(
                isHistorySelected = true,
                onHistoryClick = { /* Déjà dessus */ },
                onHomeClick = onHomeClick,
                onSettingsClick = onSettingsClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            // --- EN-TÊTE ET NAVIGATION DES MOIS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { moisSelectionne = moisSelectionne.minusMonths(1) }) {
                    Text("◀", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = moisSelectionne.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        .replaceFirstChar { it.uppercase() } + " ${moisSelectionne.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )

                IconButton(onClick = { moisSelectionne = moisSelectionne.plusMonths(1) }) {
                    Text("▶", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- ZONE SCROLLABLE POUR LE CONTENU ---
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val joursLettres = listOf("L", "M", "M", "J", "V", "S", "D")
                    joursLettres.forEach { lettre ->
                        Text(
                            text = lettre,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- CONSTRUCTION DE LA GRILLE DU CALENDRIER ---
                val premierJourDuMois = moisSelectionne.atDay(1)
                val decalageInitial = premierJourDuMois.dayOfWeek.value - 1
                val nombreJoursDansMois = moisSelectionne.lengthOfMonth()

                val totalCases = decalageInitial + nombreJoursDansMois
                val nombreLignes = if (totalCases % 7 == 0) totalCases / 7 else (totalCases / 7) + 1

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (ligne in 0 until nombreLignes) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            for (colonne in 0..6) {
                                val indexCase = ligne * 7 + colonne
                                val numeroJour = indexCase - decalageInitial + 1

                                if (indexCase < decalageInitial || numeroJour > nombreJoursDansMois) {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val dateBoucle = moisSelectionne.atDay(numeroJour)
                                    val dateBoucleString = dateBoucle.toString()

                                    val workoutAssocie = monthlyWorkouts.find { it.date == dateBoucleString }

                                    val pastilleColor = when {
                                        dateBoucle.isAfter(aujourdhui) -> Color(0xFFE0E0E0)
                                        workoutAssocie == null -> Color(0xFFE0E0E0)
                                        workoutAssocie.isCompleted -> Color(0xFF4CAF50)
                                        else -> {
                                            try {
                                                val prog = Json.decodeFromString<WorkoutProgram>(workoutAssocie.exercicesJson)
                                                if (prog.exercices.isEmpty() || workoutAssocie.sport.lowercase() == "repos") {
                                                    Color(0xFF948A30)
                                                } else {
                                                    Color(0xFFEF5350)
                                                }
                                            } catch (e: Exception) {
                                                Color(0xFFEF5350)
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(pastilleColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$numeroJour",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (pastilleColor == Color(0xFFE0E0E0)) Color.DarkGray else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                // --- 🟢 DROPDOWN 1 : STATISTIQUES SPORT ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { sportsExpanded = !sportsExpanded }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 Activités ce mois-ci",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (sportsExpanded) "▲" else "▼",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                AnimatedVisibility(visible = sportsExpanded) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
                        if (statsSports.isEmpty()) {
                            Text("Aucune séance validée ce mois-ci. Au boulot ! 💪", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            statsSports.forEach { (sportName, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("💪 $sportName", fontWeight = FontWeight.Medium)
                                    Text("$count fois", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                // --- 🟢 DROPDOWN 2 : STATISTIQUES THÈMES APPRENTISSAGE ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { themesExpanded = !themesExpanded }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🧠 Thèmes Préférés",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (themesExpanded) "▲" else "▼",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                AnimatedVisibility(visible = themesExpanded) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
                        if (statsThemes.isEmpty()) {
                            Text("Aucune leçon enregistrée sur cette période.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            statsThemes.forEach { (nom, sujet) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("💡 $nom", fontWeight = FontWeight.Medium)
                                    val badgeColor = when (sujet.lowercase()) {
                                        "science", "espace" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                                        "histoire", "politique" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
                                        "technologie", "tech" -> Color(0xFFF3E5F5) to Color(0xFF6A1B9A)
                                        else -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(color = badgeColor.first, shape = RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text =  sujet,
                                            color = badgeColor.second,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}