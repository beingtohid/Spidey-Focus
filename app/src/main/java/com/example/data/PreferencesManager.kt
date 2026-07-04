package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spidey_focus_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FOCUS_MODE_ENABLED = booleanPreferencesKey("focus_mode_enabled")
        val BEDTIME_MODE_ENABLED = booleanPreferencesKey("bedtime_mode_enabled")

        // Focus Schedule
        val FOCUS_SCHED_ENABLED = booleanPreferencesKey("focus_sched_enabled")
        val FOCUS_SCHED_START_HOUR = intPreferencesKey("focus_sched_start_hour")
        val FOCUS_SCHED_START_MIN = intPreferencesKey("focus_sched_start_min")
        val FOCUS_SCHED_END_HOUR = intPreferencesKey("focus_sched_end_hour")
        val FOCUS_SCHED_END_MIN = intPreferencesKey("focus_sched_end_min")
        val FOCUS_SCHED_DAYS = stringPreferencesKey("focus_sched_days") // csv of day of week (e.g. "2,3,4,5,6")

        // Bedtime Schedule
        val BEDTIME_SCHED_ENABLED = booleanPreferencesKey("bedtime_sched_enabled")
        val BEDTIME_SCHED_START_HOUR = intPreferencesKey("bedtime_sched_start_hour")
        val BEDTIME_SCHED_START_MIN = intPreferencesKey("bedtime_sched_start_min")
        val BEDTIME_SCHED_END_HOUR = intPreferencesKey("bedtime_sched_end_hour")
        val BEDTIME_SCHED_END_MIN = intPreferencesKey("bedtime_sched_end_min")
        val BEDTIME_SCHED_DAYS = stringPreferencesKey("bedtime_sched_days") // csv of day of week (e.g. "1,2,3,4,5,6,7")
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[ONBOARDING_COMPLETED] ?: false
    }

    val focusModeEnabledFlow: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[FOCUS_MODE_ENABLED] ?: false
    }

    val bedtimeModeEnabledFlow: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[BEDTIME_MODE_ENABLED] ?: false
    }

    // Full settings map
    val focusScheduleFlow: Flow<FocusSchedule> = context.dataStore.data.map { pref ->
        FocusSchedule(
            enabled = pref[FOCUS_SCHED_ENABLED] ?: false,
            startHour = pref[FOCUS_SCHED_START_HOUR] ?: 9,
            startMin = pref[FOCUS_SCHED_START_MIN] ?: 0,
            endHour = pref[FOCUS_SCHED_END_HOUR] ?: 17,
            endMin = pref[FOCUS_SCHED_END_MIN] ?: 0,
            days = pref[FOCUS_SCHED_DAYS] ?: "2,3,4,5,6"
        )
    }

    val bedtimeScheduleFlow: Flow<BedtimeSchedule> = context.dataStore.data.map { pref ->
        BedtimeSchedule(
            enabled = pref[BEDTIME_SCHED_ENABLED] ?: false,
            startHour = pref[BEDTIME_SCHED_START_HOUR] ?: 22,
            startMin = pref[BEDTIME_SCHED_START_MIN] ?: 0,
            endHour = pref[BEDTIME_SCHED_END_HOUR] ?: 7,
            endMin = pref[BEDTIME_SCHED_END_MIN] ?: 0,
            days = pref[BEDTIME_SCHED_DAYS] ?: "1,2,3,4,5,6,7"
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { pref ->
            pref[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setFocusModeEnabled(enabled: Boolean) {
        context.dataStore.edit { pref ->
            pref[FOCUS_MODE_ENABLED] = enabled
        }
    }

    suspend fun setBedtimeModeEnabled(enabled: Boolean) {
        context.dataStore.edit { pref ->
            pref[BEDTIME_MODE_ENABLED] = enabled
        }
    }

    suspend fun saveFocusSchedule(schedule: FocusSchedule) {
        context.dataStore.edit { pref ->
            pref[FOCUS_SCHED_ENABLED] = schedule.enabled
            pref[FOCUS_SCHED_START_HOUR] = schedule.startHour
            pref[FOCUS_SCHED_START_MIN] = schedule.startMin
            pref[FOCUS_SCHED_END_HOUR] = schedule.endHour
            pref[FOCUS_SCHED_END_MIN] = schedule.endMin
            pref[FOCUS_SCHED_DAYS] = schedule.days
        }
    }

    suspend fun saveBedtimeSchedule(schedule: BedtimeSchedule) {
        context.dataStore.edit { pref ->
            pref[BEDTIME_SCHED_ENABLED] = schedule.enabled
            pref[BEDTIME_SCHED_START_HOUR] = schedule.startHour
            pref[BEDTIME_SCHED_START_MIN] = schedule.startMin
            pref[BEDTIME_SCHED_END_HOUR] = schedule.endHour
            pref[BEDTIME_SCHED_END_MIN] = schedule.endMin
            pref[BEDTIME_SCHED_DAYS] = schedule.days
        }
    }
}

data class FocusSchedule(
    val enabled: Boolean,
    val startHour: Int,
    val startMin: Int,
    val endHour: Int,
    val endMin: Int,
    val days: String
)

data class BedtimeSchedule(
    val enabled: Boolean,
    val startHour: Int,
    val startMin: Int,
    val endHour: Int,
    val endMin: Int,
    val days: String
)
