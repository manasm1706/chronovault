package com.example.chronovault.services

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.chronovault.R
import com.example.chronovault.utils.GooglePlayServicesGuard
import com.example.chronovault.utils.NotificationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * ForegroundLocationService - Continuous location tracking
 * Runs in foreground for continuous location updates
 * Optional: Use for more frequent location checks
 */
class ForegroundLocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!GooglePlayServicesGuard.warnIfUnavailable(this, TAG)) {
            stopSelf()
            return START_NOT_STICKY
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Start foreground service
        startForegroundLocationUpdates()

        return START_STICKY
    }

    private fun startForegroundLocationUpdates() {
        // Create notification for foreground service
        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setContentTitle("ChronoVault")
            .setContentText("Tracking location for nearby memories...")
            .setSmallIcon(R.drawable.ic_home_black_24dp)
            .setColor(getColor(R.color.primary))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Request location updates
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_LOW_POWER, 30000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.d(TAG, "Location: ${location.latitude}, ${location.longitude}")
                    // Process location
                    processLocation(location.latitude, location.longitude)
                }
            }
        }

        try {
            @Suppress("MissingPermission")
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Security exception requesting location updates", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location updates: ${e.message}")
        }
    }

    private fun processLocation(latitude: Double, longitude: Double) {
        Log.d(TAG, "Processing location: $latitude, $longitude")
        // Handle location processing here
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationService"
        private const val NOTIFICATION_ID = 9999
    }
}

