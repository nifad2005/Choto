package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CompressionHistoryDao {
    @Query("SELECT * FROM compression_history WHERE status = 'COMPLETED' ORDER BY createdAtMillis DESC")
    fun observeCompletedHistory(): Flow<List<CompressionHistoryEntity>>

    @Query("SELECT * FROM compression_history WHERE status = 'COMPLETED' ORDER BY createdAtMillis DESC LIMIT :limit")
    fun observeRecentCompleted(limit: Int = 3): Flow<List<CompressionHistoryEntity>>

    @Query("SELECT * FROM compression_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CompressionHistoryEntity?

    @Query("SELECT * FROM compression_history WHERE status = 'IN_PROGRESS' ORDER BY createdAtMillis DESC LIMIT 1")
    suspend fun getLatestInProgress(): CompressionHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CompressionHistoryEntity): Long

    @Update
    suspend fun update(entity: CompressionHistoryEntity)

    @Query("DELETE FROM compression_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM compression_history WHERE status = 'IN_PROGRESS'")
    suspend fun clearInProgress()
}
