package com.example.myapplication.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.AppBottomNavigationBar

@Composable
fun PoemSettingsScreen(
    viewModel: WorkoutViewModel,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(
                isSettingsSelected = true,
                onHistoryClick = onHistoryClick,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBackClick) { Text("← Retour") }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📜 Journal de Poésie",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Retrouvez ici les poèmes qui ont accompagné vos journées.", color = Color.Gray)
            
            // On pourrait implémenter une liste ici plus tard
        }
    }
}