package com.example.myapplication.data.network

import android.content.Context
import android.content.SharedPreferences

class ApiKeyManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("api_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
        private const val KEY_NEWS_API = "news_api_key"
    }

    fun getApiKey(): String? {
        return prefs.getString(KEY_GEMINI_API, null)
    }

    fun setApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GEMINI_API, apiKey).apply()
    }

    fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrBlank()
    }

    fun getNewsApiKey(): String? {
        return prefs.getString(KEY_NEWS_API, null)
    }

    fun setNewsApiKey(apiKey: String) {
        prefs.edit().putString(KEY_NEWS_API, apiKey).apply()
    }

    fun hasNewsApiKey(): Boolean {
        return !getNewsApiKey().isNullOrBlank()
    }
}