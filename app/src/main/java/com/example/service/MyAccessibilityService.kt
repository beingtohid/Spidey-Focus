package com.example.service

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.example.MainActivity
import com.example.data.AppRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*

class MyAccessibilityService : AccessibilityService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var repository: AppRepository

    private var homePackages = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(applicationContext)
        updateHomeLauncherPackages()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val foregroundPackage = event.packageName?.toString() ?: return

        // Never block our own app, system UI, or the device launcher
        if (foregroundPackage == packageName || 
            foregroundPackage == "com.android.systemui" || 
            homePackages.contains(foregroundPackage) || 
            isPhoneOrDialer(foregroundPackage)
        ) {
            return
        }

        // Run checks in a coroutine
        serviceScope.launch {
            try {
                // 1. Clean up expired break passes first
                val now = System.currentTimeMillis()
                repository.deleteExpiredPasses(now)

                // 2. Check if a temporary break pass is active for this app
                val breakPass = repository.getBreakPass(foregroundPackage)
                if (breakPass != null && breakPass.expiryTime > now) {
                    // Allowed under temporary break pass!
                    return@launch
                }

                // 3. Check App Daily Timers (Feature 2)
                val timer = repository.getTimer(foregroundPackage)
                if (timer != null) {
                    val usageMinutes = getAppUsageTodayInMinutes(foregroundPackage)
                    if (usageMinutes >= timer.limitMinutes) {
                        launchBlockingScreen(foregroundPackage, "timer_exceeded", "Time's up! You've reached your daily limit of ${timer.limitMinutes} min for this app.")
                        return@launch
                    }
                }

                // 4. Check Focus Mode & Bedtime Mode (Feature 3 & 7)
                // Determine if Focus Mode is ON
                val isFocusOn = repository.focusModeEnabled.first()

                // Check Bedtime Mode & automatic schedule
                val isBedtimeOn = repository.bedtimeModeEnabled.first()

                val inBedtimeHours = isCurrentlyInBedtimeSchedule()
                val isDistracting = repository.isDistractingApp(foregroundPackage)

                // If distracting and (Focus Mode is on OR (Bedtime Mode is on and in Bedtime schedule)) -> Block!
                if (isDistracting) {
                    if (isFocusOn) {
                        launchBlockingScreen(foregroundPackage, "focus_mode", "Focus Mode is on — this is a distracting app. Need a quick break?")
                        return@launch
                    } else if (isBedtimeOn && inBedtimeHours) {
                        launchBlockingScreen(foregroundPackage, "bedtime_mode", "Bedtime Mode is active. Time to sleep!")
                        return@launch
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onInterrupt() {
        // Required method
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private fun launchBlockingScreen(targetPackage: String, blockType: String, message: String) {
        val intent = Intent()
        intent.setClassName(packageName, "com.example.ui.BlockingActivity")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtra("target_package", targetPackage)
        intent.putExtra("block_type", blockType)
        intent.putExtra("block_message", message)
        startActivity(intent)
    }

    private fun isPhoneOrDialer(pkg: String): Boolean {
        val dialerPkgs = setOf(
            "com.android.phone",
            "com.android.server.telecom",
            "com.google.android.dialer",
            "com.samsung.android.dialer"
        )
        if (dialerPkgs.contains(pkg)) return true
        return pkg.contains("dialer") || pkg.contains("telephony")
    }

    private fun updateHomeLauncherPackages() {
        homePackages.clear()
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        for (info in resolveInfos) {
            homePackages.add(info.activityInfo.packageName)
        }
    }

    private fun getAppUsageTodayInMinutes(targetPackage: String): Int {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0
        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        val appStat = stats.firstOrNull { it.packageName == targetPackage }
        val totalMs = appStat?.totalTimeInForeground ?: 0L
        return (totalMs / 60000L).toInt()
    }

    private suspend fun isCurrentlyInBedtimeSchedule(): Boolean {
        val schedule = repository.bedtimeSchedule.first()
        if (!schedule.enabled) return false

        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val activeDays = schedule.days.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (!activeDays.contains(currentDay)) return false

        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMin = calendar.get(Calendar.MINUTE)
        val currentTimeInMin = currentHour * 60 + currentMin

        val startMin = schedule.startHour * 60 + schedule.startMin
        val endMin = schedule.endHour * 60 + schedule.endMin

        return if (startMin < endMin) {
            currentTimeInMin >= startMin && currentTimeInMin <= endMin
        } else {
            currentTimeInMin >= startMin || currentTimeInMin <= endMin
        }
    }
}
