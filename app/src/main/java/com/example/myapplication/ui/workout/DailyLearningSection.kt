package com.example.myapplication.ui.workout

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.Info

sealed interface LearningUiState {
    object Idle : LearningUiState
    object Loading : LearningUiState
    // 🟢 AJOUT : On fait passer le booléen du Like ici
    data class Success(val info: Info, val isLiked: Boolean) : LearningUiState
    data class Error(val message: String) : LearningUiState
}

@Composable
fun DailyLearningSection(
    uiState: LearningUiState,
    onLikeClick: () -> Unit // 🟢 AJOUT : La fonction de clic qu'on transmettra au ViewModel
) {
    val context = LocalContext.current

    when (uiState) {
        is LearningUiState.Idle, is LearningUiState.Loading -> {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 3.dp)
            }
        }
        is LearningUiState.Error -> {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(text = uiState.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        is LearningUiState.Success -> {
            val savoir = uiState.info

            val badgeColor = when (savoir.sujet.lowercase()) {
                "science", "espace" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                "histoire", "politique" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
                "technologie", "tech" -> Color(0xFFF3E5F5) to Color(0xFF6A1B9A)
                else -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Ligne du haut réorganisée avec le bouton Favoris
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // 🟢 NOUVEAU : Bouton Coeur aligné tout à gauche
                        IconButton(
                            onClick = onLikeClick,
                            modifier = Modifier.align(Alignment.CenterStart).size(32.dp)
                        ) {
                            Text(
                                text = if (uiState.isLiked) "❤️" else "🤍",
                                fontSize = 20.sp
                            )
                        }

                        // Titre toujours centré au milieu
                        Text(
                            text = "💡 Le Savoir du Jour",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Badge toujours aligné tout à droite
                        Box(
                            modifier = Modifier
                                .background(color = badgeColor.first, shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .align(Alignment.CenterEnd)
                        ) {
                            Text(
                                text = savoir.sujet,
                                color = badgeColor.second,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = savoir.nom,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = savoir.explication,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 22.sp,
                        color = Color.DarkGray
                    )

                    if (savoir.liens.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "🔗 En savoir plus :",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )

                        savoir.liens.forEach { lien ->
                            Text(
                                text = lien,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lien))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}