package com.example.myapplication.data.model

import android.R
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutProgram(
    val sport: String,
    val duree_minutes: Int,
    val exercices: List<Exercise>
)

@Serializable
data class Exercise(
    val nom: String,
    val en_duree: Boolean,
    val series: Int,
    val repetitions: Int,
    val recup_secondes: Int,
    val conseil: String
)