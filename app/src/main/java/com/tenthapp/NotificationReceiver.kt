package com.example.glucoseguard
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.glucoseguard.util.GlucosePolicy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) { ReminderScheduler.schedule(context);return }
        val prefs=context.getSharedPreferences("diabetes_prefs",Context.MODE_PRIVATE)
        val key=intent.getStringExtra("reminder_type") ?: return
        if(!prefs.getBoolean("notif_master",false) || !prefs.getBoolean("notif_$key",false)) return
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return
        if(!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val pending=goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today=Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0) }.timeInMillis
                val codes=when(key) { "morning" -> setOf("fasting","breakfast_before","breakfast_after");"lunch" -> setOf("lunch_before","lunch_after");else -> setOf("dinner_before","dinner_after","bedtime") }
                val records=(context.applicationContext as DiabetesApplication).repository.allGlucoseRecords.first()
                if(records.any { it.timestamp in today..System.currentTimeMillis() && GlucosePolicy.categoryCode(it.category) in codes }) return@launch
                val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel="diabetes_reminders"
                if(Build.VERSION.SDK_INT>=26) manager.createNotificationChannel(NotificationChannel(channel,context.getString(R.string.notif_channel_name),NotificationManager.IMPORTANCE_DEFAULT))
                val tap=PendingIntent.getActivity(context,key.hashCode(),Intent(context,MainActivity::class.java).putExtra("open_glucose",true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val title=when(key){"morning"->R.string.notif_morning_title;"lunch"->R.string.notif_lunch_title;else->R.string.notif_dinner_title}
                val text=when(key){"morning"->R.string.notif_morning_content;"lunch"->R.string.notif_lunch_content;else->R.string.notif_dinner_content}
                manager.notify(key.hashCode(),NotificationCompat.Builder(context,channel).setSmallIcon(R.drawable.ic_nav_record)
                    .setContentTitle(context.getString(title)).setContentText(context.getString(text)).setContentIntent(tap).setAutoCancel(true).build())
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { android.util.Log.e("Reminder", "Unable to prepare reminder", e) }
            finally { pending.finish() }
        }
    }
}
