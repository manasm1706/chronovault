package com.example.chronovault.ui.common

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Allows six bottom-nav items for ChronoVault's tab layout.
 */
class SixItemBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {

    override fun getMaxItemCount(): Int = 6
}

