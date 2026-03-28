package com.example.chronovault.utils

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import com.example.chronovault.R

/**
 * Resolves and applies app appearance settings.
 */
object ThemeManager {

    enum class ThemeMode(val prefValue: String, val nightMode: Int) {
        SYSTEM("SYSTEM", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
        LIGHT("LIGHT", AppCompatDelegate.MODE_NIGHT_NO),
        DARK("DARK", AppCompatDelegate.MODE_NIGHT_YES)
    }

    enum class ColorScheme(val prefValue: String, val themeResId: Int) {
        METALLIC_GREEN("GREEN", R.style.Theme_ChronoVault_Scheme_MetallicGreen),
        DEEP_BLUE("BLUE", R.style.Theme_ChronoVault_Scheme_DeepBlue),
        WARM_OCHRE("OCHRE", R.style.Theme_ChronoVault_Scheme_WarmOchre),
        NEUTRAL_GRAY("GRAY", R.style.Theme_ChronoVault_Scheme_NeutralGray)
    }

    fun resolveThemeMode(value: String?): ThemeMode {
        return ThemeMode.entries.firstOrNull { it.prefValue == value } ?: ThemeMode.SYSTEM
    }

    fun resolveColorScheme(value: String?): ColorScheme {
        return ColorScheme.entries.firstOrNull { it.prefValue == value } ?: ColorScheme.METALLIC_GREEN
    }

    fun applyTheme(activity: Activity, modeValue: String?, schemeValue: String?) {
        val mode = resolveThemeMode(modeValue)
        val scheme = resolveColorScheme(schemeValue)
        AppCompatDelegate.setDefaultNightMode(mode.nightMode)
        activity.setTheme(scheme.themeResId)
    }
}

