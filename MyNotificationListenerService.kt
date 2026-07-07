package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.AppRepository
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MyNotificationListenerService : NotificationListenerService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var repository: AppRepository

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return

        // Skip our own package to prevent loop counting of status overlays
        if (pkg == packageName) return

        serviceScope.launch {
            try {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                repository.incrementNotificationCount(pkg, dateStr)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
