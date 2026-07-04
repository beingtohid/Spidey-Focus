package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // App Timers
    @Query("SELECT * FROM app_timers")
    fun getAllTimersFlow(): Flow<List<AppTimer>>

    @Query("SELECT * FROM app_timers")
    suspend fun getAllTimers(): List<AppTimer>

    @Query("SELECT * FROM app_timers WHERE packageName = :packageName LIMIT 1")
    suspend fun getTimer(packageName: String): AppTimer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimer(timer: AppTimer)

    @Query("DELETE FROM app_timers WHERE packageName = :packageName")
    suspend fun deleteTimer(packageName: String)

    @Query("DELETE FROM app_timers")
    suspend fun deleteAllTimers()


    // Distracting Apps
    @Query("SELECT * FROM distracting_apps")
    fun getAllDistractingAppsFlow(): Flow<List<DistractingApp>>

    @Query("SELECT * FROM distracting_apps")
    suspend fun getAllDistractingApps(): List<DistractingApp>

    @Query("SELECT EXISTS(SELECT 1 FROM distracting_apps WHERE packageName = :packageName)")
    suspend fun isDistractingApp(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistractingApp(app: DistractingApp)

    @Query("DELETE FROM distracting_apps WHERE packageName = :packageName")
    suspend fun deleteDistractingApp(packageName: String)


    // Break Passes
    @Query("SELECT * FROM break_passes")
    fun getAllBreakPassesFlow(): Flow<List<BreakPass>>

    @Query("SELECT * FROM break_passes")
    suspend fun getAllBreakPasses(): List<BreakPass>

    @Query("SELECT * FROM break_passes WHERE packageName = :packageName LIMIT 1")
    suspend fun getBreakPass(packageName: String): BreakPass?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreakPass(pass: BreakPass)

    @Query("DELETE FROM break_passes WHERE packageName = :packageName")
    suspend fun deleteBreakPass(packageName: String)

    @Query("DELETE FROM break_passes WHERE expiryTime < :currentTime")
    suspend fun deleteExpiredPasses(currentTime: Long)


    // Notification Counts
    @Query("SELECT * FROM notification_counts WHERE date = :date")
    fun getNotificationCountsFlow(date: String): Flow<List<NotificationCount>>

    @Query("SELECT * FROM notification_counts WHERE date = :date")
    suspend fun getNotificationCounts(date: String): List<NotificationCount>

    @Query("INSERT INTO notification_counts (packageName, date, count) VALUES (:packageName, :date, 1) ON CONFLICT(packageName, date) DO UPDATE SET count = count + 1")
    suspend fun incrementNotificationCount(packageName: String, date: String)
}
