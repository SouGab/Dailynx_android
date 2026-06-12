package com.example.myapplication.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory // 🟢 AJOUT : Pour décoder ton logo en image couleur
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R // 🟢 AJOUT : Ton fichier de ressources globales
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.model.WorkoutProgram
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.LocalDate

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val todayString = LocalDate.now().toString()
        val database = AppDatabase.getDatabase(context)
        val workoutDao = database.workoutDao()

        CoroutineScope(Dispatchers.IO).launch {
            val localWorkout = workoutDao.getWorkoutByDate(todayString)

            val title = "🏋️‍♂️ C'est l'heure du sport, Gabriel !"
            val message = if (localWorkout != null) {
                try {
                    val program = Json.decodeFromString<WorkoutProgram>(localWorkout.exercicesJson)
                    if (program.exercices.isEmpty()) {
                        "Aujourd'hui c'est Repos ! Profites-en pour bien récupérer. 😴"
                    } else {
                        "Ta séance de ${localWorkout.sport} t'attend. Durée : ${localWorkout.dureeMinutes} min. 💪"
                    }
                } catch (e: Exception) {
                    "Une séance est prévue aujourd'hui ! Ouvre l'app pour la découvrir. 🔥"
                }
            } else {
                "Pas encore de programme pour aujourd'hui ? Ouvre l'application pour le générer ! ⚡"
            }

            showNotification(context, title, message)
            NotificationScheduler.scheduleDailyNotification(context)
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val channelId = "workout_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Rappels d'entraînement",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIconBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.logo_lynx)
        } catch (e: Exception) {
            null
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setLargeIcon(largeIconBitmap)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}