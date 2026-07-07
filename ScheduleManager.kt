package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AppRepository
import kotlinx.coroutines.flow.first
import java.util.*

object ScheduleManager {

    fun scheduleAlms(context: Context, repository: AppRepository) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Run blocking on IO thread to fetch current schedules from flow safely
        kotlinx.coroutines.runBlocking {
            try {
                val focus = repository.focusSchedule.first()
                val bedtime = repository.bedtimeSchedule.first()

                // 1. Focus Schedule
                if (focus.enabled) {
                    setExactAlarm(
                        context,
                        alarmManager,
                        ScheduleReceiver.ACTION_START_FOCUS,
                        101,
                        calculateNextTrigger(focus.startHour, focus.startMin)
                    )
                    setExactAlarm(
                        context,
                        alarmManager,
                        ScheduleReceiver.ACTION_STOP_FOCUS,
                        102,
                        calculateNextTrigger(focus.endHour, focus.endMin)
                    )
                } else {
                    cancelAlarm(context, alarmManager, ScheduleReceiver.ACTION_START_FOCUS, 101)
                    cancelAlarm(context, alarmManager, ScheduleReceiver.ACTION_STOP_FOCUS, 102)
                }

                // 2. Bedtime Schedule
                if (bedtime.enabled) {
                    setExactAlarm(
                        context,
                        alarmManager,
                        ScheduleReceiver.ACTION_START_BEDTIME,
                        201,
                        calculateNextTrigger(bedtime.startHour, bedtime.startMin)
                    )
                    setExactAlarm(
                        context,
                        alarmManager,
                        ScheduleReceiver.ACTION_STOP_BEDTIME,
                        202,
                        calculateNextTrigger(bedtime.endHour, bedtime.endMin)
                    )
                } else {
                    cancelAlarm(context, alarmManager, ScheduleReceiver.ACTION_START_BEDTIME, 201)
                    cancelAlarm(context, alarmManager, ScheduleReceiver.ACTION_STOP_BEDTIME, 202)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setExactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        action: String,
        requestCode: Int,
        triggerAtMillis: Long
    ) {
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun cancelAlarm(
        context: Context,
        alarmManager: AlarmManager,
        action: String,
        requestCode: Int
    ) {
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun calculateNextTrigger(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
}
