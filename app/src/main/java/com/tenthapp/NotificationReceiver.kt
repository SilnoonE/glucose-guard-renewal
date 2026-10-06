package com.example.glucoseguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderType = intent.getStringExtra("reminder_type") ?: "general"
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "diabetes_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val title = when (reminderType) {
            "morning" -> context.getString(R.string.notif_morning_title)
            "lunch" -> context.getString(R.string.notif_lunch_title)
            "dinner" -> context.getString(R.string.notif_dinner_title)
            else -> context.getString(R.string.notif_general_title)
        }

        val content = when (reminderType) {
            "morning" -> context.getString(R.string.notif_morning_content)
            "lunch" -> context.getString(R.string.notif_lunch_content)
            "dinner" -> context.getString(R.string.notif_dinner_content)
            else -> context.getString(R.string.notif_general_content)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(reminderType.hashCode(), notification)
    }
}
