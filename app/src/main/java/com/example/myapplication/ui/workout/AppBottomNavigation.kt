package com.example.myapplication.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppBottomNavigationBar(
    isHistorySelected: Boolean = false,
    isHomeSelected: Boolean = false,
    isSettingsSelected: Boolean = false,
    onHistoryClick: () -> Unit,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 8.dp
    ) {
        // 🗓️ BOUTON HISTORIQUE (À Gauche)
        NavigationBarItem(
            selected = isHistorySelected,
            onClick = onHistoryClick,
            icon = { Text("🗓️", fontSize = 20.sp) },
            label = { Text("Historique", fontWeight = FontWeight.Medium) }
        )

        // 🐾 BOUTON HOME (Au Centre)
        NavigationBarItem(
            selected = isHomeSelected,
            onClick = onHomeClick,
            icon = { Text("🐾", fontSize = 20.sp) },
            label = { Text("Home", fontWeight = FontWeight.Bold) }
        )

        // 🛠️ BOUTON SETTINGS (À Droite)
        NavigationBarItem(
            selected = isSettingsSelected,
            onClick = onSettingsClick,
            icon = { Text("🛠️", fontSize = 20.sp) },
            label = { Text("Settings", fontWeight = FontWeight.Medium) }
        )
    }
}