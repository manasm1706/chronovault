package com.example.chronovault.services

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
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
    private var lastProcessedLocation: Location? = null
    private var lastProcessedAt: Long = 0L
    private var isLocationUpdatesActive: Boolean = false
    private val locationRequest: LocationRequest by lazy {
        LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            20_000L
        )
            .setMinUpdateIntervalMillis(15_000L)
            .setMinUpdateDistanceMeters(20f)
            .setWaitForAccurateLocation(false)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ServiceManager.isLocationServiceRunning) {
            Log.d("SERVICE_DEBUG", "ForegroundLocationService already active")
            return START_STICKY
        }

        if (!GooglePlayServicesGuard.warnIfUnavailable(this, TAG)) {
            stopSelf()
            return START_NOT_STICKY
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Start foreground service
        ServiceManager.isLocationServiceRunning = true
        Log.d("SERVICE_DEBUG", "ForegroundLocationService started")
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, notification)
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (!shouldProcessLocation(location)) {
                        continue
                    }
                    Log.d("LOCATION_DEBUG", "Location update triggered")
                    Log.d(TAG, "Location: ${location.latitude}, ${location.longitude}")
                    processLocation(location.latitude, location.longitude)
                    lastProcessedLocation = location
                    lastProcessedAt = System.currentTimeMillis()
                }
            }
        }

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (isLocationUpdatesActive) return

        try {
            Log.d("LOCATION_DEBUG", "Requesting location update")
            @Suppress("MissingPermission")
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            isLocationUpdatesActive = true
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

    private fun shouldProcessLocation(location: Location): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastProcessedAt < 20_000L) {
            return false
        }
        val previous = lastProcessedLocation ?: return true
        val distance = previous.distanceTo(location)
        return distance >= 20f
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { fusedLocationClient.removeLocationUpdates(locationCallback) }
        isLocationUpdatesActive = false
        ServiceManager.isLocationServiceRunning = false
        Log.d("SERVICE_DEBUG", "ForegroundLocationService stopped")
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationService"
        private const val NOTIFICATION_ID = 9999
    }
}

