package com.example.chronovault.utils

/**
 * Shared countdown formatting rules across dashboard and capsule surfaces.
 */
object CountdownFormatter {

    fun formatRemainingDuration(totalSeconds: Long): String {
        if (totalSeconds <= 0L) return "Unlocks in 0m"

        val daySeconds = 86_400L
        val hourSeconds = 3_600L
        val minuteSeconds = 60L

        val days = totalSeconds / daySeconds
        val hours = (totalSeconds % daySeconds) / hourSeconds
        val minutes = (totalSeconds % hourSeconds) / minuteSeconds
        val seconds = totalSeconds % minuteSeconds

        val short = when {
            days >= 365L -> {
                val years = days / 365L
                val months = (days % 365L) / 30L
                if (months > 0L) "${years}y ${months}mo" else "${years}y"
            }

            days > 30L -> {
                val months = days / 30L
                val extraDays = days % 30L
                if (extraDays > 0L) "${months}mo ${extraDays}d" else "${months}mo"
            }

            days >= 1L -> "${days}d ${hours}h"

            hours >= 1L -> "${hours}h ${minutes}m"

            minutes >= 1L -> "${minutes}m ${seconds}s"

            else -> "${seconds}s"
        }

        return "Unlocks in $short"
    }
}

