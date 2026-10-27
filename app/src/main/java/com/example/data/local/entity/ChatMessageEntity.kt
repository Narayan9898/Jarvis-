package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "USER", "JARVIS", "SYSTEM"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val language: String = "en",
    val actionId: String? = null,
    val actionSuccess: Boolean? = null,
    val actionDetail: String? = null
)
