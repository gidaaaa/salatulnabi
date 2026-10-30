package com.salatnabi.app
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences("salat_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("reminder_enabled", false)) return

        val serviceIntent = Intent(context, ReminderService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Reschedule next
        val minutes = prefs.getInt("minutes", 15)
        val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val pi = android.app.PendingIntent.getBroadcast(context, 0, Intent(context, ReminderReceiver::class.java), android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        val next = System.currentTimeMillis() + minutes * 60 * 1000L
        try {
            am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, next, pi)
        } catch (e: Exception) {
            am.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, next, pi)
        }
    }
}