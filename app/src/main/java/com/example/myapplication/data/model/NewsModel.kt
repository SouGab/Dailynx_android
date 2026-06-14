package com.example.myapplication.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NewsResponse(val articles: List<Article>)

@Serializable
data class Article(
    val title: String,
    val description: String? = null,
    val url: String,
    val source: Source? = null,
    val publishedAt: String? = null
)

@Serializable
data class Source(val name: String)
