package com.example.chronovault.utils

/**
 * Shared countdown formatting rules across dashboard and capsule surfaces.
 */
object CountdownFormatter {

    fun formatRemainingDuration(totalSeconds: Long): String {
        val daySeconds = 86_400L
        val hourSeconds = 3_600L
        val minuteSeconds = 60L

        val days = totalSeconds / daySeconds
        val hours = (totalSeconds % daySeconds) / hourSeconds
        val minutes = (totalSeconds % hourSeconds) / minuteSeconds
        val seconds = totalSeconds % minuteSeconds

        return when {
            // For long timers, show only year/month/day granularity.
            days > 30L -> {
                val years = days / 365L
                val months = (days % 365L) / 30L
                val remainingDays = (days % 365L) % 30L
                val parts = mutableListOf<String>()
                if (years > 0L) parts += "${years}y"
                if (months > 0L) parts += "${months}mo"
                if (remainingDays > 0L || parts.isEmpty()) parts += "${remainingDays}d"
                parts.joinToString(" ")
            }
            // Between one week and one month: days only.
            days >= 7L -> "${days}d"
            // Under a week but at least 24h: include hours.
            totalSeconds >= 24L * hourSeconds -> {
                if (days > 0L) "${days}d ${hours}h" else "${hours}h"
            }
            // Under 24h: include minutes and seconds.
            else -> "${hours}h ${minutes}m ${seconds}s"
        }
    }
}

