package com.example.myapplication.data.network

import android.content.Context
import android.content.SharedPreferences

class ApiKeyManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("api_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
        private const val KEY_NEWS_API = "news_api_key"
        
        private const val KEY_SHOW_SPORT = "show_sport"
        private const val KEY_SHOW_SAVOIR = "show_savoir"
        private const val KEY_SHOW_POEM = "show_poem"
        private const val KEY_SHOW_NEWS = "show_news"
    }

    fun isSportEnabled(): Boolean = prefs.getBoolean(KEY_SHOW_SPORT, true)
    fun setSportEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SHOW_SPORT, enabled).apply()

    fun isSavoirEnabled(): Boolean = prefs.getBoolean(KEY_SHOW_SAVOIR, true)
    fun setSavoirEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SHOW_SAVOIR, enabled).apply()

    fun isPoemEnabled(): Boolean = prefs.getBoolean(KEY_SHOW_POEM, true)
    fun setPoemEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SHOW_POEM, enabled).apply()

    fun isNewsEnabled(): Boolean = prefs.getBoolean(KEY_SHOW_NEWS, true)
    fun setNewsEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SHOW_NEWS, enabled).apply()

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