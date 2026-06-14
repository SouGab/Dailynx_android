package com.example.myapplication.data.network

import android.util.Log
import com.example.myapplication.data.model.NewsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class NewsService(private val apiKey: String) {
    private val client = OkHttpClient()
    private val baseUrl = "https://newsapi.org/v2/top-headlines?country=fr&apiKey="
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchFrenchNews(): NewsResponse? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(baseUrl + apiKey).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                json.decodeFromString<NewsResponse>(body)
            }
        } catch (e: Exception) {
            Log.e("NewsService", "Error fetching news", e)
            null
        }
    }
}
