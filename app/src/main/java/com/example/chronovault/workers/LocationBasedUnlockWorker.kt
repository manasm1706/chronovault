package com.example.chronovault.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.utils.NotificationHelper
import com.example.chronovault.utils.GooglePlayServicesGuard
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks

/**
 * LocationBasedUnlockWorker - Check for capsules within proximity
 * Periodically checks user location and unlocks nearby capsules
 */
class LocationBasedUnlockWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    override suspend fun doWork(): Result {
        return try {
            val capsuleRepository = ServiceLocator.provideCapsuleRepository(applicationContext)
            val preferencesManager = ServiceLocator.providePreferencesManager(applicationContext)
            val notificationRepository = ServiceLocator.provideNotificationRepository(applicationContext)
            val nearbyCooldownMillis = 15 * 60 * 1000L

            if (!preferencesManager.isLocationTrackingEnabled()) {
                return Result.success()
            }

            if (!GooglePlayServicesGuard.warnIfUnavailable(applicationContext, "LocationUnlockWorker")) {
                return Result.success()
            }

            // Get current location
            val location = Tasks.await(fusedLocationClient.lastLocation)
            if (location != null) {
                val userLatitude = location.latitude
                val userLongitude = location.longitude

                // Get location-based capsules
                val locationBasedCapsules = capsuleRepository.getLocationBasedCapsules()

                locationBasedCapsules.forEach { capsule ->
                    // Calculate distance
                    val distance = calculateDistance(
                        userLatitude, userLongitude,
                        capsule.latitude, capsule.longitude
                    )

                    // FIX: 15
                    // If within 50 meters, unlock
                    if (distance <= 50f && !capsule.isUnlocked) {
                        if (preferencesManager.shouldRunCooldownEvent("worker_nearby_${capsule.id}", nearbyCooldownMillis)) {
                            NotificationHelper.sendNearbyCapsuleAlert(applicationContext)
                        }
                    }

                    if (distance <= 50f && !capsule.isUnlocked) {
                        capsuleRepository.unlockCapsule(capsule.id)

                        if (capsule.isSharedWithMe && !capsule.isDiscovered) {
                            capsuleRepository.markCapsuleDiscovered(capsule.id)
                        }

                        // Send notification
                        NotificationHelper.sendLocationBasedUnlockNotification(
                            applicationContext,
                            capsule.title,
                            "You're near \"${capsule.title}\"! Unlock it now."
                        )

                        notificationRepository.createUnlockNotification(
                            capsuleId = capsule.id,
                            capsuleTitle = capsule.title,
                            source = "location"
                        )
                    }
                }
            }

            Result.success()
        } catch (_: SecurityException) {
            // Recoverable environment issue (Play services broker/security mismatch). Avoid retry loop.
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val earthRadiusMeters = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return (earthRadiusMeters * c).toFloat()
    }

    companion object {
        const val WORK_NAME = "location_based_unlock_work"
    }
}

