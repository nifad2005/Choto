package com.example.compression

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.example.ChotoApplication
import com.example.MainActivity
import com.example.R
import com.example.data.CompressionHistoryEntity
import com.example.model.CompressionConfig
import com.example.model.CompressionEstimate
import com.example.model.VideoFormatOption
import com.example.model.VideoMetadata
import com.example.util.BanglaFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
class CompressionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var transformer: Transformer? = null
    private var progressRunnable: Runnable? = null
    private var activeHistoryId: Long = 0L
    private var activeOutputFile: File? = null
    private var startTimeMillis: Long = 0L
    private var isCancelled: Boolean = false
    private var hasRetriedH264Fallback: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                handleCancel()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                val metadata = CompressionCoordinator.pendingMetadata
                val config = CompressionCoordinator.pendingConfig
                val estimate = CompressionCoordinator.pendingEstimate
                if (metadata == null || config == null || estimate == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                ensureNotificationChannel()
                startForegroundWithNotification(
                    fileName = metadata.displayName,
                    progress = 0,
                )
                beginCompression(metadata, config, estimate)
            }
        }
        return START_NOT_STICKY
    }

    private fun beginCompression(
        metadata: VideoMetadata,
        config: CompressionConfig,
        estimate: CompressionEstimate,
    ) {
        isCancelled = false
        hasRetriedH264Fallback = false
        startTimeMillis = System.currentTimeMillis()

        val compressedDir = File(cacheDir, "compressed").apply { mkdirs() }
        val outputFile = File(compressedDir, "choto_${System.currentTimeMillis()}.mp4")
        activeOutputFile = outputFile

        serviceScope.launch {
            val historyRepo = (application as ChotoApplication).historyRepository
            historyRepo.clearInProgress()
            val entity = CompressionHistoryEntity(
                originalName = metadata.displayName,
                originalUri = metadata.uriString,
                outputPath = outputFile.absolutePath,
                savedUri = null,
                thumbnailPath = metadata.thumbnailPath,
                originalSizeBytes = metadata.sizeBytes,
                compressedSizeBytes = 0L,
                durationMs = metadata.durationMs,
                width = metadata.width,
                height = metadata.height,
                presetName = config.preset.name,
                isSaved = false,
                status = CompressionHistoryEntity.STATUS_IN_PROGRESS,
                createdAtMillis = startTimeMillis,
            )
            activeHistoryId = historyRepo.insert(entity)

            val initialMimeType = when (config.format) {
                VideoFormatOption.MP4_HEVC -> MimeTypes.VIDEO_H265
                VideoFormatOption.MP4_H264 -> MimeTypes.VIDEO_H264
                VideoFormatOption.WEBM -> MimeTypes.VIDEO_H264
            }
            launchTransformer(
                metadata = metadata,
                config = config,
                estimate = estimate,
                outputFile = outputFile,
                mimeType = initialMimeType,
            )
        }
    }

    private fun launchTransformer(
        metadata: VideoMetadata,
        config: CompressionConfig,
        estimate: CompressionEstimate,
        outputFile: File,
        mimeType: String,
    ) {
        stopProgressPolling()
        transformer?.cancel()
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val mediaItem = MediaItem.fromUri(Uri.parse(metadata.uriString))
        val videoEffects = buildList<Effect> {
            val targetShort = estimate.targetShortSide
            if (targetShort != null && metadata.shortSide > targetShort) {
                val targetHeight = if (metadata.height <= metadata.width) {
                    targetShort
                } else {
                    val scaledH = (targetShort.toFloat() * metadata.height / metadata.width).roundToInt()
                    if (scaledH % 2 == 0) scaledH else scaledH + 1
                }
                add(Presentation.createForHeight(targetHeight.coerceAtLeast(240)))
            }
        }

        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setRemoveAudio(!config.keepAudio)
            .setEffects(Effects(emptyList(), videoEffects))
            .build()

        val encoderFactory = DefaultEncoderFactory.Builder(this)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(estimate.targetVideoBitrateBps)
                    .build(),
            )
            .setEnableFallback(true)
            .build()

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                if (isCancelled) return
                stopProgressPolling()
                onExportCompleted(metadata, outputFile)
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException,
            ) {
                if (isCancelled) return
                stopProgressPolling()
                if (mimeType != MimeTypes.VIDEO_H264 && !hasRetriedH264Fallback) {
                    hasRetriedH264Fallback = true
                    launchTransformer(
                        metadata = metadata,
                        config = config,
                        estimate = estimate,
                        outputFile = outputFile,
                        mimeType = MimeTypes.VIDEO_H264,
                    )
                    return
                }
                onExportFailed()
            }
        }

        val builtTransformer = Transformer.Builder(this)
            .setVideoMimeType(mimeType)
            .setEncoderFactory(encoderFactory)
            .addListener(listener)
            .build()

        transformer = builtTransformer
        builtTransformer.start(editedMediaItem, outputFile.absolutePath)
        startProgressPolling(builtTransformer, metadata, estimate)
    }

    private fun startProgressPolling(
        activeTransformer: Transformer,
        metadata: VideoMetadata,
        estimate: CompressionEstimate,
    ) {
        val progressHolder = ProgressHolder()
        val runnable = object : Runnable {
            override fun run() {
                if (isCancelled) return
                val state = activeTransformer.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    val pct = progressHolder.progress.coerceIn(0, 99)
                    val elapsedMs = System.currentTimeMillis() - startTimeMillis
                    val remainingSec = if (pct >= 4 && elapsedMs >= 1500L) {
                        val totalEstimatedMs = (elapsedMs * 100.0) / pct
                        val rem = ((totalEstimatedMs - elapsedMs) / 1000.0).roundToInt().toLong()
                        if (rem > 10L) rem else null
                    } else {
                        null
                    }

                    CompressionCoordinator.updateState(
                        CompressionJobState.Running(
                            historyId = activeHistoryId,
                            fileName = metadata.displayName,
                            progressPercent = pct,
                            remainingSeconds = remainingSec,
                            isFinishing = pct >= 95,
                            originalSizeBytes = metadata.sizeBytes,
                            estimatedSizeBytes = estimate.estimatedBytes,
                        ),
                    )
                    updateNotification(metadata.displayName, pct)
                }
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    mainHandler.postDelayed(this, 120L)
                }
            }
        }
        progressRunnable = runnable
        mainHandler.postDelayed(runnable, 120L)
    }

    private fun stopProgressPolling() {
        progressRunnable?.let { mainHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun onExportCompleted(metadata: VideoMetadata, outputFile: File) {
        serviceScope.launch {
            // Ensure minimum 400ms animation so fast jobs don't flash awkwardly
            val elapsed = System.currentTimeMillis() - startTimeMillis
            if (elapsed < 420L) {
                delay(420L - elapsed)
            }
            if (isCancelled) return@launch

            val historyRepo = (application as ChotoApplication).historyRepository
            val outputSize = outputFile.length()

            if (outputSize <= 0L || (metadata.sizeBytes > 0L && outputSize >= metadata.sizeBytes)) {
                runCatching { outputFile.delete() }
                historyRepo.deleteById(activeHistoryId)
                CompressionCoordinator.updateState(
                    CompressionJobState.Failed(
                        messageResId = R.string.err_no_change,
                        isNoChange = true,
                    ),
                )
                finishService()
                return@launch
            }

            val existing = historyRepo.getById(activeHistoryId)
            val thumb = existing?.thumbnailPath ?: VideoInspector.generateThumbnailForFile(
                this@CompressionService,
                outputFile.absolutePath,
            )
            if (existing != null) {
                historyRepo.update(
                    existing.copy(
                        outputPath = outputFile.absolutePath,
                        thumbnailPath = thumb,
                        compressedSizeBytes = outputSize,
                        status = CompressionHistoryEntity.STATUS_COMPLETED,
                    ),
                )
            }

            CompressionCoordinator.updateState(
                CompressionJobState.Completed(
                    historyId = activeHistoryId,
                    originalSizeBytes = metadata.sizeBytes,
                    compressedSizeBytes = outputSize,
                ),
            )
            finishService()
        }
    }

    private fun onExportFailed() {
        serviceScope.launch {
            runCatching { activeOutputFile?.delete() }
            val historyRepo = (application as ChotoApplication).historyRepository
            historyRepo.deleteById(activeHistoryId)
            CompressionCoordinator.updateState(
                CompressionJobState.Failed(
                    messageResId = R.string.err_generic,
                    isNoChange = false,
                ),
            )
            finishService()
        }
    }

    private fun handleCancel() {
        isCancelled = true
        stopProgressPolling()
        runCatching { transformer?.cancel() }
        transformer = null
        runCatching { activeOutputFile?.delete() }
        serviceScope.launch {
            val historyRepo = (application as ChotoApplication).historyRepository
            if (activeHistoryId > 0L) {
                historyRepo.deleteById(activeHistoryId)
            } else {
                historyRepo.clearInProgress()
            }
            CompressionCoordinator.resetToIdle()
            finishService()
        }
    }

    private fun finishService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = getString(R.string.notif_channel_desc)
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun startForegroundWithNotification(fileName: String, progress: Int) {
        val notification = buildNotification(fileName, progress)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(fileName: String, progress: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(fileName, progress))
    }

    private fun buildNotification(fileName: String, progress: Int): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val cancelIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CompressionService::class.java).apply {
                action = ACTION_CANCEL
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val percentStr = BanglaFormatter.formatPercent(progress)
        val contentText = getString(R.string.notif_progress_text, percentStr)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setColor(ACCENT_COLOR_INT)
            .setContentTitle(getString(R.string.state_compressing))
            .setContentText("$contentText · ${BanglaFormatter.middleEllipsize(fileName, 24)}")
            .setProgress(100, progress.coerceIn(0, 100), progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.cta_cancel), cancelIntent)
            .build()
    }

    override fun onDestroy() {
        stopProgressPolling()
        runCatching { transformer?.cancel() }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.compression.ACTION_START"
        const val ACTION_CANCEL = "com.example.compression.ACTION_CANCEL"
        private const val CHANNEL_ID = "choto_compression_channel"
        private const val NOTIFICATION_ID = 4102
        private const val ACCENT_COLOR_INT = 0xFF1F7A55.toInt()
    }
}
