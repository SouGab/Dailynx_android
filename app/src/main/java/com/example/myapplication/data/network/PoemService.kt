package com.example.myapplication.data.network

import android.util.Log
import com.example.myapplication.data.model.Poem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class PoemService {
    private val client = OkHttpClient()
    private val url = "https://poetrydb.org/random/"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchRandomPoems(number: Int): List<Poem> = withContext(Dispatchers.IO) {
        try {
            Log.i("PoemService", "Poems fetched successfully")
            val request = Request.Builder().url(url + number).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                json.decodeFromString<List<Poem>>(body)
            }
        } catch (e: Exception) {
            Log.e("PoemService", "Error fetching poems", e)
            emptyList()
        }
    }
}