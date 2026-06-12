package com.example.myapplication.data.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateManager(private val context: Context) {
    private val client = OkHttpClient()
    private val repoUrl = "https://api.github.com/repos/SouGab/Dailynx_android"
    private val apkUrl = "https://github.com/SouGab/Dailynx_android/releases/download/latest/app-debug.apk"
    private val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_SHA = "last_commit_sha"
    }

    suspend fun checkForUpdates(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$repoUrl/commits/main")
                .header("User-Agent", "Dailynx-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("UpdateManager", "Response not successful: ${response.code}")
                    return@withContext null
                }
                val bodyString = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyString)
                val latestSha = json.getString("sha")
                
                // On vérifie le SHA enregistré.
                val currentSha = prefs.getString(KEY_LAST_SHA, null)

                // Si le SHA GitHub est différent du dernier SHA enregistré, on propose la maj.
                if (currentSha != null && latestSha != currentSha) {
                    return@withContext latestSha
                }
                
                // Si c'est la toute première fois qu'on lance l'app sans SHA enregistré,
                // on enregistre le SHA actuel de GitHub comme étant la version courante.
                if (currentSha == null) {
                    updateCurrentSha(latestSha)
                }
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Error checking for updates", e)
        }
        null
    }

    fun updateCurrentSha(sha: String) {
        prefs.edit().putString(KEY_LAST_SHA, sha).apply()
    }

    suspend fun downloadAndInstallApk(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(context.externalCacheDir, "update.apk")
            if (file.exists()) file.delete()

            val request = Request.Builder()
                .url(apkUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                
                response.body?.byteStream()?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            if (!file.exists()) return@withContext false

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return@withContext true
        } catch (e: Exception) {
            Log.e("UpdateManager", "Error downloading/installing APK", e)
            return@withContext false
        }
    }
}