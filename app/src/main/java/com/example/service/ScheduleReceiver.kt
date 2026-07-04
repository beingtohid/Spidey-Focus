package com.example.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ScheduleReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_START_FOCUS = "com.example.ACTION_START_FOCUS"
        const val ACTION_STOP_FOCUS = "com.example.ACTION_STOP_FOCUS"
        const val ACTION_START_BEDTIME = "com.example.ACTION_START_BEDTIME"
        const val ACTION_STOP_BEDTIME = "com.example.ACTION_STOP_BEDTIME"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val repository = AppRepository(context.applicationContext)
        val action = intent.action ?: return

        receiverScope.launch {
            try {
                when (action) {
                    ACTION_START_FOCUS -> {
                        repository.setFocusModeEnabled(true)
                        startFocusForegroundService(context)
                    }
                    ACTION_STOP_FOCUS -> {
                        repository.setFocusModeEnabled(false)
                        stopFocusForegroundService(context)
                    }
                    ACTION_START_BEDTIME -> {
                        repository.setBedtimeModeEnabled(true)
                        toggleDoNotDisturb(context, true)
                    }
                    ACTION_STOP_BEDTIME -> {
                        repository.setBedtimeModeEnabled(false)
                        toggleDoNotDisturb(context, false)
                    }
                }
                
                // Reschedule for next day/occurrence
                ScheduleManager.scheduleAlms(context, repository)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startFocusForegroundService(context: Context) {
        val serviceIntent = Intent(context, FocusService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun stopFocusForegroundService(context: Context) {
        val serviceIntent = Intent(context, FocusService::class.java)
        context.stopService(serviceIntent)
    }

    private fun toggleDoNotDisturb(context: Context, enable: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                if (enable) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                } else {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            }
        }
    }
}
