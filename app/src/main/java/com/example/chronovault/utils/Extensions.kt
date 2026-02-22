package com.example.chronovault.utils

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.chronovault.R

/**
 * Extension functions for common UI operations
 */

/**
 * Set status badge styling based on status type
 */
fun TextView.setStatusBadge(status: String) {
    text = when (status) {
        "locked" -> "🔒 Locked"
        "unlocked" -> "🔓 Unlocked"
        "shared" -> "👥 Shared"
        else -> status
    }

    val (bgColor, textColor) = when (status) {
        "locked" -> Pair(R.color.warning, R.color.text_primary)
        "unlocked" -> Pair(R.color.success, R.color.white)
        "shared" -> Pair(R.color.primary_light, R.color.white)
        else -> Pair(R.color.surface_variant, R.color.text_secondary)
    }

    setBackgroundColor(ContextCompat.getColor(context, bgColor))
    setTextColor(ContextCompat.getColor(context, textColor))
    setPadding(12, 6, 12, 6)
}

/**
 * Format date to human-readable format
 */
fun Long.formatDate(): String {
    val date = java.util.Date(this)
    val format = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return format.format(date)
}

/**
 * Format date with time
 */
fun Long.formatDateTime(): String {
    val date = java.util.Date(this)
    val format = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}

/**
 * Validate email format
 */
fun String.isValidEmail(): Boolean {
    return this.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))
}

/**
 * Check if password is strong (at least 6 characters)
 */
fun String.isValidPassword(): Boolean {
    return this.length >= 6
}

