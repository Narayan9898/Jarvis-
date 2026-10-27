package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jarvis_memory")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // "PREFERENCE", "CONTACT_NICKNAME", "STYLE", "FACT"
    val key: String,
    val value: String,
    val updatedTimestamp: Long = System.currentTimeMillis()
)
