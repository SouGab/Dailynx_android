package com.example.myapplication.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Poem(
    val title: String,
    val author: String,
    val lines: List<String>,
    val linecount: String
)