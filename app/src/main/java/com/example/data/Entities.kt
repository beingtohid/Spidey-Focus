package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_timers")
data class AppTimer(
    @PrimaryKey val packageName: String,
    val limitMinutes: Int
)

@Entity(tableName = "distracting_apps")
data class DistractingApp(
    @PrimaryKey val packageName: String
)

@Entity(tableName = "break_passes")
data class BreakPass(
    @PrimaryKey val packageName: String,
    val expiryTime: Long
)

@Entity(tableName = "notification_counts", primaryKeys = ["packageName", "date"])
data class NotificationCount(
    val packageName: String,
    val date: String, // "yyyy-MM-dd"
    val count: Int
)
