package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppRepository
import kotlinx.coroutines.*

class FocusService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var repository: AppRepository

    companion object {
        const val CHANNEL_ID = "focus_mode_channel"
        const val NOTIFICATION_ID = 4554
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP_FOCUS = "ACTION_STOP_FOCUS"
    }

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_FOCUS) {
            serviceScope.launch {
                repository.setFocusModeEnabled(false)
                withContext(Dispatchers.Main) {
                    stopSelf()
                }
            }
            return START_NOT_STICKY
        }

        startForegroundServiceNotification()
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val stopIntent = Intent(this, FocusService::class.java).apply {
            action = ACTION_STOP_FOCUS
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tapping notification opens the main app
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Mode is On")
            .setContentText("Distracting apps are paused.")
            .setSmallIcon(android.R.drawable.ic_lock_lock) // system fallback icon or we can use custom
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Turn off",
                stopPendingIntent
            )
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Mode Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when Focus Mode is active and distracting apps are paused."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
