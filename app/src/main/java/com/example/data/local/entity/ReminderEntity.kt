package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val type: String = "REMINDER", // "REMINDER", "ALARM", "TIMER"
    val timestamp: Long,
    val timeFormatted: String,
    val isCompleted: Boolean = false,
    val recurrence: String? = null,
    val notes: String? = null
)
