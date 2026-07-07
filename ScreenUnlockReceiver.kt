package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ScreenUnlockReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        val UNLOCK_DATE = stringPreferencesKey("unlock_date")
        val UNLOCK_COUNT = intPreferencesKey("unlock_count")
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            scope.launch {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val today = sdf.format(Date())

                    context.dataStore.edit { pref ->
                        val storedDate = pref[UNLOCK_DATE] ?: ""
                        if (storedDate == today) {
                            val currentCount = pref[UNLOCK_COUNT] ?: 0
                            pref[UNLOCK_COUNT] = currentCount + 1
                        } else {
                            pref[UNLOCK_DATE] = today
                            pref[UNLOCK_COUNT] = 1
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
