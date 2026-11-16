package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "compression_history")
data class CompressionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val originalName: String,
    val originalUri: String,
    val outputPath: String?,
    val savedUri: String?,
    val thumbnailPath: String?,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val presetName: String,
    val isSaved: Boolean,
    val status: String, // "IN_PROGRESS", "COMPLETED", "FAILED"
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    val savedPercent: Int
        get() = if (originalSizeBytes > 0L && compressedSizeBytes in 1 until originalSizeBytes) {
            (((originalSizeBytes - compressedSizeBytes) * 100.0) / originalSizeBytes).toInt().coerceIn(1, 99)
        } else {
            0
        }

    companion object {
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
    }
}
