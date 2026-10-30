package com.salatnabi.app
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ReminderService : Service() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification()
        playCustomAudio()
        // Stop after 20 seconds
        android.os.Handler(mainLooper).postDelayed({ stopSelf() }, 20000)
        return START_NOT_STICKY
    }

    private fun showNotification() {
        val channelId = "salat_channel_v2"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "تذكير الصلاة على النبي", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "تذكير دوري بالصلاة على النبي"
            channel.enableVibration(true)
            nm.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("صَلِّ على النبي ﷺ")
            .setContentText("اللهم صل وسلم وبارك على نبينا محمد - اضغط للصلاة")
            .setStyle(NotificationCompat.BigTextStyle().bigText("اللهم صل وسلم وبارك على نبينا محمد وعلى آله وصحبه أجمعين. اذكر الله وصل على النبي."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingOpen)
            .setOngoing(false)
            .build()

        startForeground(1, notification)
        // Also show as regular notification so it stays
        nm.notify(1001, notification)
    }

    private fun playCustomAudio() {
        try {
            val prefs = getSharedPreferences("salat_prefs", MODE_PRIVATE)
            val uriStr = prefs.getString("audio_uri", null) ?: return
            val uri = Uri.parse(uriStr)
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, uri)
            mediaPlayer?.setOnCompletionListener { stopSelf() }
            mediaPlayer?.start()
        } catch (e: Exception) {
            // fallback - just notification
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}