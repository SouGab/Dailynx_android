package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_poems")
data class PoemEntity(
    @PrimaryKey val date: String,
    val title: String,
    val author: String,
    val linesJson: String
)