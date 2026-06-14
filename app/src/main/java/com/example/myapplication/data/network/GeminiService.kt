package com.example.myapplication.data.network

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService(apiKey: String) {
    private val config = generationConfig { responseMimeType = "application/json" }
    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.5-flash",
        apiKey = apiKey,
        generationConfig = config,
    )

    suspend fun fetchWorkoutJson(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.i("GEMINI", "L'IA est appelé pour généré votre entrainement")
            Log.i("PROMPT", prompt)
            generativeModel.generateContent(prompt).text
        } catch (e: Exception) {
            Log.e("GEMINI_ERROR", "Erreur lors de l'appel API", e)
            null
        }
    }
}