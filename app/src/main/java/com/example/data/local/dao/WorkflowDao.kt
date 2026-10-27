package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.WorkflowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {
    @Query("SELECT * FROM workflows ORDER BY id ASC")
    fun getAllWorkflowsFlow(): Flow<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE isEnabled = 1")
    suspend fun getEnabledWorkflows(): List<WorkflowEntity>

    @Query("SELECT * FROM workflows WHERE LOWER(triggerPhrase) = LOWER(:phrase) LIMIT 1")
    suspend fun findWorkflowByTrigger(phrase: String): WorkflowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: WorkflowEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWorkflows(workflows: List<WorkflowEntity>)

    @Update
    suspend fun updateWorkflow(workflow: WorkflowEntity)

    @Delete
    suspend fun deleteWorkflow(workflow: WorkflowEntity)

    @Query("UPDATE workflows SET lastRunTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastRun(id: Long, timestamp: Long)
}
