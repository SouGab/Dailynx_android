package com.example.myapplication.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import com.example.myapplication.ui.components.LynxLogo
import com.example.myapplication.ui.components.AppBottomNavigationBar

@Composable
fun HomeScreen(
    viewModel: WorkoutViewModel,
    onStartWorkoutClick: () -> Unit,
    onSettingsClick: () -> Unit, // 🟢 Renommé car il pointe vers Settings
    onHistoryClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val completedDates by viewModel.completedDates.collectAsState()
    val learningState by viewModel.learningState.collectAsState()
    val poemState by viewModel.poemState.collectAsState()

    val aujourdhui = remember { LocalDate.now() }
    val lundiDeLaSemaine = remember { aujourdhui.with(DayOfWeek.MONDAY) }
    var jourSelectionne by remember { mutableStateOf(aujourdhui) }

    val scrollState = rememberScrollState()

    LaunchedEffect(jourSelectionne) {
        viewModel.loadOrCreateDailyProgram(
            contraintes = "Douleur poignet",
            dateString = jourSelectionne.toString(),
            sport = null
        )
    }

    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(
                isHomeSelected = jourSelectionne.isEqual(aujourdhui),
                onHistoryClick = onHistoryClick,
                onHomeClick = { jourSelectionne = aujourdhui },
                onSettingsClick = onSettingsClick // 🟢 Envoie vers la page Settings
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // --- EN-TÊTE PREMIUM ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Salut Gabriel!",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "HomeScreen",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
                LynxLogo(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- SECTION 1 : LE CALENDRIER HEBDOMADAIRE ---
            Text("Cette Semaine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                items(7) { index ->
                    val dateDuJour = remember { lundiDeLaSemaine.plusDays(index.toLong()) }
                    val nomDuJour = remember { dateDuJour.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceFirstChar { it.uppercase() } }

                    val estAujourdhui = dateDuJour.isEqual(aujourdhui)
                    val estFait = completedDates.contains(dateDuJour.toString())
                    val estSelectionne = dateDuJour.isEqual(jourSelectionne)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(CircleShape)
                            .clickable { jourSelectionne = dateDuJour }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        Text(nomDuJour, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = when {
                                        estFait -> Color(0xFF4CAF50)
                                        estAujourdhui -> MaterialTheme.colorScheme.primary
                                        else -> Color(0xFFE0E0E0)
                                    },
                                    shape = CircleShape
                                )
                                .border(
                                    width = if (estSelectionne) 3.dp else 0.dp,
                                    color = if (estSelectionne) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = "${dateDuJour.dayOfMonth}",
                                color = if (estAujourdhui || estFait) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // --- SECTION 2 : ZONE DE SCROLL CENTRALE ---
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState),
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                DailyWorkoutSection(uiState = uiState, jourSelectionne = jourSelectionne, aujourdhui = aujourdhui, onStartWorkoutClick = onStartWorkoutClick)
                Spacer(modifier = Modifier.height(20.dp))
                DailyLearningSection(uiState = learningState, onLikeClick = { viewModel.toggleLikeLearning(jourSelectionne.toString()) })
                Spacer(modifier = Modifier.height(20.dp))
                DailyPoemSection(uiState = poemState)
                Spacer(modifier = Modifier.height(24.dp)) // Espace net en bas du scroll
            }
        }
    }
}