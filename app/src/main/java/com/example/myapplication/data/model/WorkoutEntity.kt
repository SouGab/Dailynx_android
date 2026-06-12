package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_workouts")
data class WorkoutEntity(
    @PrimaryKey
    val date: String,
    val sport: String,
    val dureeMinutes: Int,
    val exercicesJson: String,
    val isCompleted: Boolean = false
)