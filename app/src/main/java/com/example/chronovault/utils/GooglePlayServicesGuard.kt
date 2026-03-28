package com.example.chronovault.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Small guard to avoid calling Play Services location APIs when GMS is unavailable.
 */
object GooglePlayServicesGuard {

    fun isAvailable(context: Context): Boolean {
        val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        return status == ConnectionResult.SUCCESS
    }

    fun warnIfUnavailable(context: Context, tag: String): Boolean {
        val available = isAvailable(context)
        if (!available) {
            val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
            Log.w(tag, "Google Play services unavailable (status=$status). Skipping fused location call.")
        }
        return available
    }
}

