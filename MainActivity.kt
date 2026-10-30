package com.salatnabi.app
import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.io.File

class MainActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var customAudioUri: Uri? = null
    private var counter = 0
    private var isRecording = false
    private val prefs by lazy { getSharedPreferences("salat_prefs", MODE_PRIVATE) }

    private val pickAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            customAudioUri = it
            prefs.edit().putString("audio_uri", it.toString()).putBoolean("is_custom_file", true).apply()
            findViewById<TextView>(R.id.audioPathText).text = "✓ تم اختيار: ${it.lastPathSegment ?: "ملف صوتي"}"
            Toast.makeText(this, "تم حفظ الصوت", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        // handle
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermissions()

        counter = prefs.getInt("counter", 0)
        val minutesInput = findViewById<EditText>(R.id.minutesInput)
        val counterText = findViewById<TextView>(R.id.counterText)
        val salatBtn = findViewById<MaterialButton>(R.id.salatBtn)
        val switch = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.reminderSwitch)
        val uploadBtn = findViewById<MaterialButton>(R.id.uploadAudioBtn)
        val recordBtn = findViewById<MaterialButton>(R.id.recordBtn)
        val audioPath = findViewById<TextView>(R.id.audioPathText)

        counterText.text = "صليت اليوم: $counter مرة"
        minutesInput.setText(prefs.getInt("minutes", 15).toString())
        val savedUri = prefs.getString("audio_uri", null)
        if (savedUri != null) {
            customAudioUri = Uri.parse(savedUri)
            audioPath.text = "✓ صوت محفوظ"
        }
        switch.isChecked = prefs.getBoolean("reminder_enabled", false)

        fun setMinutes(m: Int) {
            minutesInput.setText(m.toString())
            prefs.edit().putInt("minutes", m).apply()
            if (switch.isChecked) schedule(m)
        }

        findViewById<MaterialButton>(R.id.btn5).setOnClickListener { setMinutes(5) }
        findViewById<MaterialButton>(R.id.btn15).setOnClickListener { setMinutes(15) }
        findViewById<MaterialButton>(R.id.btn30).setOnClickListener { setMinutes(30) }
        findViewById<MaterialButton>(R.id.btn60).setOnClickListener { setMinutes(60) }

        salatBtn.setOnClickListener {
            counter++
            prefs.edit().putInt("counter", counter).apply()
            counterText.text = "صليت اليوم: $counter مرة"
            playAudio()
        }

        uploadBtn.setOnClickListener { pickAudio.launch("audio/*") }

        recordBtn.setOnClickListener {
            if (!isRecording) startRecording(recordBtn) else stopRecording(recordBtn)
        }

        switch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("reminder_enabled", checked).apply()
            if (checked) {
                val mins = minutesInput.text.toString().toIntOrNull() ?: 15
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val am = getSystemService(ALARM_SERVICE) as AlarmManager
                    if (!am.canScheduleExactAlarms()) {
                        Toast.makeText(this, "يرجى السماح بضبط المنبه الدقيق من الإعدادات", Toast.LENGTH_LONG).show()
                        startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        return@setOnCheckedChangeListener
                    }
                }
                schedule(mins)
            } else {
                cancel()
            }
        }
    }

    private fun checkPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.READ_MEDIA_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.RECORD_AUDIO)
        if (perms.isNotEmpty()) requestPermissionLauncher.launch(perms.toTypedArray())
    }

    private fun playAudio() {
        mediaPlayer?.release()
        mediaPlayer = try {
            customAudioUri?.let { MediaPlayer.create(this, it) }
        } catch (e: Exception) { null }
        if (mediaPlayer == null) {
            // Fallback default beep / silent
            Toast.makeText(this, "ﷺ اللهم صل على محمد", Toast.LENGTH_SHORT).show()
            return
        }
        mediaPlayer?.start()
    }

    private fun startRecording(btn: MaterialButton) {
        try {
            val file = File(filesDir, "my_salat_${System.currentTimeMillis()}.m4a")
            mediaRecorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare(); start()
            }
            isRecording = true
            btn.text = "⏹ إيقاف التسجيل..."
            prefs.edit().putString("recorded_path", file.absolutePath).apply()
        } catch (e: Exception) {
            Toast.makeText(this, "فشل التسجيل: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording(btn: MaterialButton) {
        try {
            mediaRecorder?.stop(); mediaRecorder?.release(); mediaRecorder = null
            isRecording = false
            btn.text = "سجل صوتك الآن"
            val path = prefs.getString("recorded_path", null)
            if (path != null) {
                customAudioUri = Uri.fromFile(File(path))
                prefs.edit().putString("audio_uri", customAudioUri.toString()).apply()
                findViewById<TextView>(R.id.audioPathText).text = "✓ تم حفظ تسجيلك"
                Toast.makeText(this, "تم حفظ تسجيلك وسيعمل في الخلفية", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {}
    }

    private fun schedule(minutes: Int) {
        val intent = Intent(this, ReminderReceiver::class.java)
        val pi = android.app.PendingIntent.getBroadcast(this, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + minutes * 60 * 1000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms())
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            else
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
        Toast.makeText(this, "سيتم تذكيرك كل $minutes دقيقة حتى بعد إغلاق التطبيق", Toast.LENGTH_LONG).show()
    }

    private fun cancel() {
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        val pi = android.app.PendingIntent.getBroadcast(this, 0, Intent(this, ReminderReceiver::class.java), android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        am.cancel(pi)
        Toast.makeText(this, "تم إيقاف التذكير", Toast.LENGTH_SHORT).show()
    }
}