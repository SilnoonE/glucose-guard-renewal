package com.example.glucoseguard
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
object ReminderScheduler {
    private val keys=listOf("morning","lunch","dinner")
    fun schedule(context: Context) {
        val prefs=context.getSharedPreferences("diabetes_prefs",Context.MODE_PRIVATE)
        val manager=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        keys.forEachIndexed { index,key ->
            val pending=PendingIntent.getBroadcast(context,index,Intent(context,NotificationReceiver::class.java).putExtra("reminder_type",key),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.cancel(pending)
            if(prefs.getBoolean("notif_master",false) && prefs.getBoolean("notif_$key",false)) {
                val parts=(prefs.getString("notif_${key}_time",if(index==0) "08:00" else if(index==1) "12:00" else "18:00") ?: "08:00").split(":")
                val hour=parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0,23) ?: 8
                val minute=parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0,59) ?: 0
                val next=Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,hour);set(Calendar.MINUTE,minute);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0);if(timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR,1) }
                manager.setInexactRepeating(AlarmManager.RTC_WAKEUP,next.timeInMillis,AlarmManager.INTERVAL_DAY,pending)
            }
        }
    }
}
