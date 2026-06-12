package com.example.myapplication.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneId

object NotificationScheduler {

    fun scheduleDailyNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = LocalDateTime.now()
        var targetTime = now.withHour(19).withMinute(30).withSecond(0).withNano(0)

        // Si 18h est déjà passé aujourd'hui, on bascule sur demain
        if (now.isAfter(targetTime)) {
            targetTime = targetTime.plusDays(1)
        }

        val triggerAtMillis = targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // 'setAndAllowWhileIdle' garantit le déclenchement même si le téléphone économise sa batterie (Doze mode)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }
}