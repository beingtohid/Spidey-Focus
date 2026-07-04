package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class AppRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.appDao()
    private val preferencesManager = PreferencesManager(context)

    // Room Database Observables
    val allTimersFlow: Flow<List<AppTimer>> = dao.getAllTimersFlow()
    val allDistractingAppsFlow: Flow<List<DistractingApp>> = dao.getAllDistractingAppsFlow()
    val allBreakPassesFlow: Flow<List<BreakPass>> = dao.getAllBreakPassesFlow()

    fun getNotificationCountsFlow(date: String): Flow<List<NotificationCount>> {
        return dao.getNotificationCountsFlow(date)
    }

    // DataStore Observables
    val focusModeEnabled: Flow<Boolean> = preferencesManager.focusModeEnabledFlow
    val bedtimeModeEnabled: Flow<Boolean> = preferencesManager.bedtimeModeEnabledFlow
    val focusSchedule: Flow<FocusSchedule> = preferencesManager.focusScheduleFlow
    val bedtimeSchedule: Flow<BedtimeSchedule> = preferencesManager.bedtimeScheduleFlow
    val onboardingCompleted: Flow<Boolean> = preferencesManager.onboardingCompletedFlow

    // Room Database Modifiers
    suspend fun getAllTimers(): List<AppTimer> = dao.getAllTimers()
    suspend fun getTimer(packageName: String): AppTimer? = dao.getTimer(packageName)
    suspend fun insertTimer(timer: AppTimer) = dao.insertTimer(timer)
    suspend fun deleteTimer(packageName: String) = dao.deleteTimer(packageName)
    suspend fun deleteAllTimers() = dao.deleteAllTimers()

    suspend fun getAllDistractingApps(): List<DistractingApp> = dao.getAllDistractingApps()
    suspend fun isDistractingApp(packageName: String): Boolean = dao.isDistractingApp(packageName)
    suspend fun insertDistractingApp(packageName: String) = dao.insertDistractingApp(DistractingApp(packageName))
    suspend fun deleteDistractingApp(packageName: String) = dao.deleteDistractingApp(packageName)

    suspend fun getAllBreakPasses(): List<BreakPass> = dao.getAllBreakPasses()
    suspend fun getBreakPass(packageName: String): BreakPass? = dao.getBreakPass(packageName)
    suspend fun insertBreakPass(packageName: String, expiryTime: Long) = dao.insertBreakPass(BreakPass(packageName, expiryTime))
    suspend fun deleteBreakPass(packageName: String) = dao.deleteBreakPass(packageName)
    suspend fun deleteExpiredPasses(currentTime: Long) = dao.deleteExpiredPasses(currentTime)

    suspend fun getNotificationCounts(date: String): List<NotificationCount> = dao.getNotificationCounts(date)
    suspend fun incrementNotificationCount(packageName: String, date: String) = dao.incrementNotificationCount(packageName, date)

    // DataStore Modifiers
    suspend fun setOnboardingCompleted(completed: Boolean) = preferencesManager.setOnboardingCompleted(completed)
    suspend fun setFocusModeEnabled(enabled: Boolean) = preferencesManager.setFocusModeEnabled(enabled)
    suspend fun setBedtimeModeEnabled(enabled: Boolean) = preferencesManager.setBedtimeModeEnabled(enabled)
    suspend fun saveFocusSchedule(schedule: FocusSchedule) = preferencesManager.saveFocusSchedule(schedule)
    suspend fun saveBedtimeSchedule(schedule: BedtimeSchedule) = preferencesManager.saveBedtimeSchedule(schedule)
}
