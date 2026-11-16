package com.example.model

import kotlin.math.max
import kotlin.math.roundToLong

enum class CompressionPreset {
    BEST,
    BALANCED,
    SMALLEST,
    CUSTOM,
}

enum class ResolutionOption(val targetShortSide: Int?) {
    AUTO(null),
    P1080(1080),
    P720(720),
    P480(480),
}

enum class BitrateOption {
    AUTO,
    LOW,
    MEDIUM,
    HIGH,
}

enum class VideoFormatOption {
    MP4_H264,
    MP4_HEVC,
    WEBM,
}

enum class FpsOption(val fps: Int) {
    FPS_24(24),
    FPS_30(30),
    FPS_60(60),
}

data class CompressionConfig(
    val preset: CompressionPreset = CompressionPreset.BALANCED,
    val resolution: ResolutionOption = ResolutionOption.AUTO,
    val bitrate: BitrateOption = BitrateOption.AUTO,
    val format: VideoFormatOption = VideoFormatOption.MP4_H264,
    val fps: FpsOption = FpsOption.FPS_30,
    val keepAudio: Boolean = true,
) {
    companion object {
        fun forPreset(preset: CompressionPreset, sourceSizeBytes: Long = 0L, hevcSupported: Boolean = true): CompressionConfig {
            val useHevc = hevcSupported && sourceSizeBytes > 100L * 1024L * 1024L
            return when (preset) {
                CompressionPreset.BEST -> CompressionConfig(
                    preset = CompressionPreset.BEST,
                    resolution = ResolutionOption.AUTO,
                    bitrate = BitrateOption.HIGH,
                    format = if (useHevc) VideoFormatOption.MP4_HEVC else VideoFormatOption.MP4_H264,
                    fps = FpsOption.FPS_30,
                    keepAudio = true,
                )
                CompressionPreset.BALANCED -> CompressionConfig(
                    preset = CompressionPreset.BALANCED,
                    resolution = ResolutionOption.P720,
                    bitrate = BitrateOption.MEDIUM,
                    format = if (useHevc) VideoFormatOption.MP4_HEVC else VideoFormatOption.MP4_H264,
                    fps = FpsOption.FPS_30,
                    keepAudio = true,
                )
                CompressionPreset.SMALLEST -> CompressionConfig(
                    preset = CompressionPreset.SMALLEST,
                    resolution = ResolutionOption.P480,
                    bitrate = BitrateOption.LOW,
                    format = if (hevcSupported) VideoFormatOption.MP4_HEVC else VideoFormatOption.MP4_H264,
                    fps = FpsOption.FPS_24,
                    keepAudio = true,
                )
                CompressionPreset.CUSTOM -> CompressionConfig(
                    preset = CompressionPreset.CUSTOM,
                )
            }
        }
    }
}

data class VideoMetadata(
    val uriString: String,
    val displayName: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val bitrateBps: Long,
    val fps: Int,
    val hasAudio: Boolean,
    val thumbnailPath: String?,
) {
    val shortSide: Int get() = minOf(width, height).coerceAtLeast(1)
    val isSmallFile: Boolean get() = sizeBytes in 1 until (5L * 1024L * 1024L)
    val isLongVideo: Boolean get() = durationMs > 30L * 60L * 1000L
    val resolutionLabel: String
        get() = when {
            shortSide >= 2160 -> "4K"
            shortSide >= 1080 -> "1080p"
            shortSide >= 720 -> "720p"
            shortSide >= 480 -> "480p"
            else -> "${shortSide}p"
        }
}

data class CompressionEstimate(
    val estimatedBytes: Long,
    val targetVideoBitrateBps: Int,
    val targetShortSide: Int?,
    val savedPercent: Int,
    val willActuallyShrink: Boolean,
)

object CompressionEstimator {
    private const val AUDIO_BITRATE_BPS = 128_000L

    fun estimate(metadata: VideoMetadata, config: CompressionConfig): CompressionEstimate {
        val durationSec = max(1.0, metadata.durationMs / 1000.0)
        val sourceBitrate = if (metadata.bitrateBps > 0L) {
            metadata.bitrateBps
        } else {
            ((metadata.sizeBytes * 8.0) / durationSec).roundToLong().coerceAtLeast(500_000L)
        }

        val targetShortSide = resolveTargetShortSide(metadata.shortSide, config)
        val resScaleFactor = if (targetShortSide != null && metadata.shortSide > 0) {
            (targetShortSide.toDouble() / metadata.shortSide.toDouble()).coerceIn(0.25, 1.0)
        } else {
            1.0
        }

        val basePresetBitrate = when (config.bitrate) {
            BitrateOption.HIGH -> (sourceBitrate * 0.68).roundToLong().coerceIn(900_000L, 6_000_000L)
            BitrateOption.MEDIUM -> (sourceBitrate * 0.42).roundToLong().coerceIn(550_000L, 2_800_000L)
            BitrateOption.LOW -> (sourceBitrate * 0.22).roundToLong().coerceIn(320_000L, 1_100_000L)
            BitrateOption.AUTO -> when (config.preset) {
                CompressionPreset.BEST -> (sourceBitrate * 0.68).roundToLong().coerceIn(900_000L, 6_000_000L)
                CompressionPreset.BALANCED -> (sourceBitrate * 0.42).roundToLong().coerceIn(550_000L, 2_800_000L)
                CompressionPreset.SMALLEST -> (sourceBitrate * 0.22).roundToLong().coerceIn(320_000L, 1_100_000L)
                CompressionPreset.CUSTOM -> (sourceBitrate * 0.45).roundToLong().coerceIn(450_000L, 3_000_000L)
            }
        }

        val resolutionAdjustedBitrate = (basePresetBitrate * (0.55 + 0.45 * resScaleFactor)).roundToLong()
        val codecFactor = when (config.format) {
            VideoFormatOption.MP4_HEVC -> 0.78
            VideoFormatOption.WEBM -> 0.85
            VideoFormatOption.MP4_H264 -> 1.0
        }
        val fpsFactor = when (config.fps) {
            FpsOption.FPS_24 -> 0.90
            FpsOption.FPS_30 -> 1.0
            FpsOption.FPS_60 -> 1.28
        }

        val targetVideoBitrate = (resolutionAdjustedBitrate * codecFactor * fpsFactor)
            .roundToLong()
            .coerceIn(220_000L, 12_000_000L)

        val effectiveAudioBps = if (config.keepAudio && metadata.hasAudio) AUDIO_BITRATE_BPS else 0L
        val rawEstimatedBytes = (((targetVideoBitrate + effectiveAudioBps) * durationSec) / 8.0)
            .roundToLong()
            .coerceAtLeast(32_000L)

        val willShrink = rawEstimatedBytes < metadata.sizeBytes
        val savedPercent = if (metadata.sizeBytes > 0L && willShrink) {
            (((metadata.sizeBytes - rawEstimatedBytes) * 100.0) / metadata.sizeBytes).toInt().coerceIn(1, 95)
        } else {
            0
        }

        return CompressionEstimate(
            estimatedBytes = rawEstimatedBytes,
            targetVideoBitrateBps = targetVideoBitrate.toInt(),
            targetShortSide = targetShortSide,
            savedPercent = savedPercent,
            willActuallyShrink = willShrink,
        )
    }

    private fun resolveTargetShortSide(sourceShortSide: Int, config: CompressionConfig): Int? {
        val requested = when (config.resolution) {
            ResolutionOption.AUTO -> when (config.preset) {
                CompressionPreset.BEST -> null
                CompressionPreset.BALANCED -> 720
                CompressionPreset.SMALLEST -> 480
                CompressionPreset.CUSTOM -> null
            }
            else -> config.resolution.targetShortSide
        }
        return if (requested != null && sourceShortSide > requested) requested else null
    }
}
