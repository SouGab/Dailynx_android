package com.example.myapplication.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Info(
    val nom: String,
    val sujet: String,       // ex: Science, Histoire, Technologie, Économie...
    val explication: String, // Rapide explication d'un paragraphe
    val liens: List<String>  // Liens d'approfondissement (ex: Wikipédia, YouTube...)
)

// Structure pour recevoir la semaine complète d'apprentissage
@Serializable
data class DailyLearningInput(
    val jour: String,
    val info: Info
)

@Serializable
data class WeeklyLearningResponse(
    val lecons: List<DailyLearningInput>
)