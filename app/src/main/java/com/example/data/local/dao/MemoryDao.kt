package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM jarvis_memory ORDER BY updatedTimestamp DESC")
    fun getAllMemoryFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM jarvis_memory WHERE category = :category")
    suspend fun getMemoryByCategory(category: String): List<MemoryEntity>

    @Query("SELECT * FROM jarvis_memory WHERE `key` = :key LIMIT 1")
    suspend fun getMemoryByKey(key: String): MemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMemory(memory: MemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM jarvis_memory WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM jarvis_memory")
    suspend fun clearAllMemory()
}
