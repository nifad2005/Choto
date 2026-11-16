package com.example

import com.example.model.CompressionConfig
import com.example.model.CompressionEstimator
import com.example.model.CompressionPreset
import com.example.model.VideoMetadata
import com.example.util.BanglaFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BanglaFormatterTest {

    @Test
    fun `bn converts all digits to Bangla numerals`() {
        assertEquals("০১২৩৪৫৬৭৮৯", BanglaFormatter.bn("0123456789"))
        assertEquals("৬৪%", BanglaFormatter.formatPercent(64))
        assertEquals("১০:২৪", BanglaFormatter.formatDuration(624_000L))
    }

    @Test
    fun `formatSizeCompactMb formats MB in Bangla digits`() {
        val bytes124Mb = 124L * 1024L * 1024L
        assertEquals("১২৪ MB", BanglaFormatter.formatSizeCompactMb(bytes124Mb))
        assertEquals("২৮ মেগাবাইট", BanglaFormatter.formatSizeSentenceMb(28L * 1024L * 1024L))
    }

    @Test
    fun `middleEllipsize keeps extension intact`() {
        val name = "আমার_বন্ধুদের_কক্সবাজার_ট্যুর_ভিডিও_২০২৬.mp4"
        val truncated = BanglaFormatter.middleEllipsize(name, 22)
        assertTrue(truncated.endsWith(".mp4"))
        assertTrue(truncated.contains("…"))
    }

    @Test
    fun `estimator predicts smaller size across presets`() {
        val meta = VideoMetadata(
            uriString = "content://video/1",
            displayName = "sample.mp4",
            sizeBytes = 124L * 1024L * 1024L,
            durationMs = 60_000L,
            width = 1920,
            height = 1080,
            bitrateBps = 16_000_000L,
            fps = 30,
            hasAudio = true,
            thumbnailPath = null,
        )
        val best = CompressionEstimator.estimate(meta, CompressionConfig.forPreset(CompressionPreset.BEST))
        val balanced = CompressionEstimator.estimate(meta, CompressionConfig.forPreset(CompressionPreset.BALANCED))
        val smallest = CompressionEstimator.estimate(meta, CompressionConfig.forPreset(CompressionPreset.SMALLEST))

        assertTrue(best.willActuallyShrink)
        assertTrue(balanced.estimatedBytes < best.estimatedBytes)
        assertTrue(smallest.estimatedBytes < balanced.estimatedBytes)
    }

    @Test
    fun `estimator detects when video cannot shrink meaningfully`() {
        val tiny = VideoMetadata(
            uriString = "content://video/2",
            displayName = "tiny.mp4",
            sizeBytes = 150_000L,
            durationMs = 60_000L,
            width = 480,
            height = 360,
            bitrateBps = 20_000L,
            fps = 24,
            hasAudio = true,
            thumbnailPath = null,
        )
        val best = CompressionEstimator.estimate(tiny, CompressionConfig.forPreset(CompressionPreset.BEST))
        assertFalse(best.willActuallyShrink)
    }
}
