package com.salatnabi.app
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = context.getSharedPreferences("salat_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("reminder_enabled", false)) return
            val minutes = prefs.getInt("minutes", 15)
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(context, 0, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val next = System.currentTimeMillis() + minutes * 60 * 1000L
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        }
    }
}