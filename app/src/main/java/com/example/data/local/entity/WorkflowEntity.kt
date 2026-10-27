package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val triggerPhrase: String,
    val description: String,
    val stepsJson: String, // JSON array of action step objects
    val isEnabled: Boolean = true,
    val lastRunTimestamp: Long? = null
)
