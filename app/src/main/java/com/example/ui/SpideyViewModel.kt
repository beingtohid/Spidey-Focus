package com.example.ui

import android.app.AppOpsManager
import android.app.Application
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.service.FocusService
import com.example.service.MyAccessibilityService
import com.example.service.MyNotificationListenerService
import com.example.service.ScheduleManager
import com.example.service.ScreenUnlockReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class AppInfoItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val usageMinutes: Int,
    val notificationCount: Int,
    val limitMinutes: Int?, // null if none
    val isDistracting: Boolean
)

data class DashboardState(
    val totalScreenTimeMinutes: Int = 0,
    val weeklyScreenTime: List<Pair<String, Float>> = emptyList(),
    val appUsageList: List<AppInfoItem> = emptyList(),
    val unlockCountToday: Int = 0,
    val isRefreshing: Boolean = false,
    val isPermissionMissingOnboard: Boolean = false
)

class SpideyViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val repository = AppRepository(context)

    // UI States
    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    val onboardingCompleted: StateFlow<Boolean> = repository.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val focusModeEnabled: StateFlow<Boolean> = repository.focusModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val bedtimeModeEnabled: StateFlow<Boolean> = repository.bedtimeModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val focusSchedule: StateFlow<FocusSchedule> = repository.focusSchedule
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusSchedule(false, 9, 0, 17, 0, "2,3,4,5,6"))

    val bedtimeSchedule: StateFlow<BedtimeSchedule> = repository.bedtimeSchedule
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BedtimeSchedule(false, 22, 0, 7, 0, "1,2,3,4,5,6,7"))

    init {
        refreshDashboard()
        // Listen to active status and update service or receiver triggers
        viewModelScope.launch {
            focusModeEnabled.collect { isEnabled ->
                toggleFocusService(isEnabled)
            }
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _dashboardState.update { it.copy(isRefreshing = true) }
            val hasUsage = hasUsageAccess()
            
            if (!hasUsage) {
                _dashboardState.update { 
                    it.copy(isPermissionMissingOnboard = true, isRefreshing = false) 
                }
                return@launch
            }

            // Fetch app statistics and usage info
            val appUsage = getAppUsageList()
            val totalMinutes = appUsage.sumOf { it.usageMinutes }
            val weeklyTime = getWeeklyScreenTime()
            val unlocks = getUnlockCount()

            _dashboardState.update {
                it.copy(
                    totalScreenTimeMinutes = totalMinutes,
                    weeklyScreenTime = weeklyTime,
                    appUsageList = appUsage,
                    unlockCountToday = unlocks,
                    isRefreshing = false,
                    isPermissionMissingOnboard = false
                )
            }
        }
    }

    // Installed apps fetching & processing
    private suspend fun getAppUsageList(): List<AppInfoItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        
        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        // Get daily stats
        val statsMap = usageStatsManager?.queryAndAggregateUsageStats(startTime, endTime) ?: emptyMap()

        // Get launcher apps only (to keep it professional and ignore internal packages)
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolvedApps = pm.queryIntentActivities(mainIntent, 0)

        // Get limits, distracting apps, notification counts
        val timers = repository.getAllTimers().associateBy { it.packageName }
        val distracting = repository.getAllDistractingApps().map { it.packageName }.toSet()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val notifications = repository.getNotificationCounts(dateStr).associateBy { it.packageName }

        val list = resolvedApps.mapNotNull { resolve ->
            val pkg = resolve.activityInfo.packageName
            val label = resolve.activityInfo.loadLabel(pm).toString()
            val icon = resolve.activityInfo.loadIcon(pm)

            val usageStats = statsMap[pkg]
            val totalTimeMs = usageStats?.totalTimeInForeground ?: 0L
            val minutes = (totalTimeMs / 60000L).toInt()

            val limit = timers[pkg]?.limitMinutes
            val isDist = distracting.contains(pkg)
            val notifCount = notifications[pkg]?.count ?: 0

            AppInfoItem(
                packageName = pkg,
                appName = label,
                icon = icon,
                usageMinutes = minutes,
                notificationCount = notifCount,
                limitMinutes = limit,
                isDistracting = isDist
            )
        }

        // Sort by usage time descending
        list.sortedByDescending { it.usageMinutes }
    }

    private suspend fun getWeeklyScreenTime(): List<Pair<String, Float>> = withContext(Dispatchers.IO) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return@withContext emptyList()
        val list = mutableListOf<Pair<String, Float>>()
        val sdf = SimpleDateFormat("E", Locale.getDefault())

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val start = dayCal.timeInMillis
            val end = start + 24 * 60 * 60 * 1000L

            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            val totalMs = stats.sumOf { it.totalTimeInForeground }
            val minutes = totalMs / 60000f
            list.add(Pair(sdf.format(dayCal.time), minutes))
        }
        list
    }

    private suspend fun getUnlockCount(): Int = withContext(Dispatchers.IO) {
        val pref = context.dataStore.data.first()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        val storedDate = pref[ScreenUnlockReceiver.UNLOCK_DATE] ?: ""
        if (storedDate == today) {
            pref[ScreenUnlockReceiver.UNLOCK_COUNT] ?: 0
        } else {
            0
        }
    }

    // Toggle Focus Mode
    fun toggleFocusMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setFocusModeEnabled(enabled)
        }
    }

    // Toggle Bedtime Mode
    fun toggleBedtimeMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBedtimeModeEnabled(enabled)
        }
    }

    // Onboarding complete
    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted(true)
        }
    }

    // Distracting app lists modifiers
    fun toggleDistractingApp(packageName: String, isDistracting: Boolean) {
        viewModelScope.launch {
            if (isDistracting) {
                repository.insertDistractingApp(packageName)
            } else {
                repository.deleteDistractingApp(packageName)
            }
            refreshDashboard()
        }
    }

    // App timer limits modifiers
    fun setAppLimit(packageName: String, minutes: Int) {
        viewModelScope.launch {
            repository.insertTimer(AppTimer(packageName, minutes))
            refreshDashboard()
        }
    }

    fun removeAppLimit(packageName: String) {
        viewModelScope.launch {
            repository.deleteTimer(packageName)
            refreshDashboard()
        }
    }

    // Schedule updates
    fun updateFocusSchedule(schedule: FocusSchedule) {
        viewModelScope.launch {
            repository.saveFocusSchedule(schedule)
            ScheduleManager.scheduleAlms(context, repository)
        }
    }

    fun updateBedtimeSchedule(schedule: BedtimeSchedule) {
        viewModelScope.launch {
            repository.saveBedtimeSchedule(schedule)
            ScheduleManager.scheduleAlms(context, repository)
        }
    }

    // Service launcher helper
    private fun toggleFocusService(enabled: Boolean) {
        val serviceIntent = Intent(context, FocusService::class.java)
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            context.stopService(serviceIntent)
        }
    }

    // Permission checks
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isAccessibilityEnabled(): Boolean {
        val expectedComponentName = ComponentName(context, MyAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentName)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }

    fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(context, MyNotificationListenerService::class.java)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }

    fun hasDndAccess(): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }
    }

    fun hasPostNotificationsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
