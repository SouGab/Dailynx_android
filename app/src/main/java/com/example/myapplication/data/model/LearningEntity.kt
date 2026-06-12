package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_learning")
data class LearningEntity(
    @PrimaryKey val date: String, // "AAAA-MM-JJ"
    val infoJson: String,          // L'objet Info entier converti en texte JSON
    val isLiked: Boolean = false,
) {
}