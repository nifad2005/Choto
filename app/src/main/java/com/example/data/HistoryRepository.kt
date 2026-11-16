package com.example.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: CompressionHistoryDao) {
    val recentCompleted: Flow<List<CompressionHistoryEntity>> = dao.observeRecentCompleted(3)
    val allCompleted: Flow<List<CompressionHistoryEntity>> = dao.observeCompletedHistory()

    suspend fun getById(id: Long): CompressionHistoryEntity? = dao.getById(id)

    suspend fun getLatestInProgress(): CompressionHistoryEntity? = dao.getLatestInProgress()

    suspend fun insert(entity: CompressionHistoryEntity): Long = dao.insert(entity)

    suspend fun update(entity: CompressionHistoryEntity) = dao.update(entity)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun restore(entity: CompressionHistoryEntity): Long = dao.insert(entity)

    suspend fun clearInProgress() = dao.clearInProgress()
}
