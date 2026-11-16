package com.example.compression

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.CompressionConfig
import com.example.model.CompressionEstimate
import com.example.model.VideoMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface CompressionJobState {
    data object Idle : CompressionJobState

    data class Running(
        val historyId: Long,
        val fileName: String,
        val progressPercent: Int,
        val remainingSeconds: Long?,
        val isFinishing: Boolean,
        val originalSizeBytes: Long,
        val estimatedSizeBytes: Long,
    ) : CompressionJobState

    data class Completed(
        val historyId: Long,
        val originalSizeBytes: Long,
        val compressedSizeBytes: Long,
    ) : CompressionJobState

    data class Failed(
        val messageResId: Int? = null,
        val isNoChange: Boolean = false,
    ) : CompressionJobState
}

object CompressionCoordinator {
    private val _jobState = MutableStateFlow<CompressionJobState>(CompressionJobState.Idle)
    val jobState: StateFlow<CompressionJobState> = _jobState.asStateFlow()

    @Volatile
    var pendingConfig: CompressionConfig? = null

    @Volatile
    var pendingMetadata: VideoMetadata? = null

    @Volatile
    var pendingEstimate: CompressionEstimate? = null

    fun updateState(state: CompressionJobState) {
        _jobState.value = state
    }

    fun resetToIdle() {
        _jobState.value = CompressionJobState.Idle
    }

    fun startCompression(
        context: Context,
        metadata: VideoMetadata,
        config: CompressionConfig,
        estimate: CompressionEstimate,
    ) {
        pendingMetadata = metadata
        pendingConfig = config
        pendingEstimate = estimate
        _jobState.value = CompressionJobState.Running(
            historyId = 0L,
            fileName = metadata.displayName,
            progressPercent = 0,
            remainingSeconds = null,
            isFinishing = false,
            originalSizeBytes = metadata.sizeBytes,
            estimatedSizeBytes = estimate.estimatedBytes,
        )
        val intent = Intent(context, CompressionService::class.java).apply {
            action = CompressionService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun cancelCompression(context: Context) {
        val intent = Intent(context, CompressionService::class.java).apply {
            action = CompressionService.ACTION_CANCEL
        }
        context.startService(intent)
    }
}
