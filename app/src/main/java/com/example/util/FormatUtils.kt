package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object FormatUtils {
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return if (digitGroups == 0) {
            "$bytes B"
        } else {
            String.format(Locale.US, "%.2f %s", value, units[digitGroups])
        }
    }

    fun formatIecBytes(bytes: Long): String {
        if (bytes <= 0L) return ""
        val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return if (digitGroups == 0) {
            "$bytes B"
        } else {
            String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }
    }

    fun formatDuration(millis: Long): String {
        val totalSec = (millis / 1000L).coerceAtLeast(0L)
        val hours = totalSec / 3600L
        val mins = (totalSec % 3600L) / 60L
        val secs = totalSec % 60L
        return when {
            hours > 0 -> String.format(Locale.US, "%dh %02dm %02ds", hours, mins, secs)
            mins > 0 -> String.format(Locale.US, "%dm %02ds", mins, secs)
            else -> String.format(Locale.US, "%.1fs", millis / 1000.0)
        }
    }

    fun formatEta(seconds: Long): String {
        if (seconds <= 0L) return "< 1s"
        val mins = seconds / 60L
        val secs = seconds % 60L
        return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
    }

    fun formatTimestamp(millis: Long): String {
        if (millis <= 0L) return "Unknown"
        val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatShortDate(millis: Long): String {
        if (millis <= 0L) return ""
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
