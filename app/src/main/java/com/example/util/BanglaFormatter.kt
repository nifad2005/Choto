package com.example.util

import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

object BanglaFormatter {
    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun bn(value: Int): String = bn(value.toString())

    fun bn(value: Long): String = bn(value.toString())

    fun bn(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(banglaDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatSizeCompactMb(bytes: Long): String {
        val safeBytes = max(0L, bytes)
        val mb = safeBytes / (1024.0 * 1024.0)
        return if (mb >= 10.0) {
            "${bn(mb.roundToInt())} MB"
        } else if (mb >= 1.0) {
            val formatted = String.format(Locale.US, "%.1f", mb).removeSuffix(".0")
            "${bn(formatted)} MB"
        } else {
            val tenths = max(0.1, (safeBytes / (1024.0 * 1024.0)))
            val formatted = if (safeBytes == 0L) "0" else String.format(Locale.US, "%.1f", tenths)
            "${bn(formatted)} MB"
        }
    }

    fun formatSizeSentenceMb(bytes: Long): String {
        val safeBytes = max(0L, bytes)
        val mb = max(1, (safeBytes / (1024.0 * 1024.0)).roundToInt())
        return "${bn(mb)} মেগাবাইট"
    }

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = max(0L, durationMs / 1000L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        val raw = if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
        return bn(raw)
    }

    fun formatPercent(percent: Int): String {
        val clamped = percent.coerceIn(0, 100)
        return "${bn(clamped)}%"
    }

    fun formatRemainingTime(seconds: Long): String {
        val safeSec = max(1L, seconds)
        return if (safeSec < 60L) {
            "প্রায় ${bn(safeSec)} সেকেন্ড বাকি"
        } else {
            val mins = max(1L, (safeSec + 30L) / 60L)
            "প্রায় ${bn(mins)} মিনিট বাকি"
        }
    }

    fun middleEllipsize(fileName: String, maxChars: Int = 28): String {
        if (fileName.length <= maxChars || maxChars < 8) return fileName
        val dotIndex = fileName.lastIndexOf('.')
        val ext = if (dotIndex in 1 until fileName.length - 1 && fileName.length - dotIndex <= 6) {
            fileName.substring(dotIndex)
        } else {
            ""
        }
        val base = if (ext.isNotEmpty()) fileName.substring(0, dotIndex) else fileName
        val available = maxChars - ext.length - 1 // 1 for ellipsis
        if (available <= 4) return fileName.take(maxChars - 1) + "…"
        val headLen = (available * 0.65).toInt().coerceAtLeast(3)
        val tailLen = (available - headLen).coerceAtLeast(1)
        return "${base.take(headLen)}…${base.takeLast(tailLen)}$ext"
    }
}
