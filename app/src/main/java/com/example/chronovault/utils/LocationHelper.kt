package com.example.chronovault.utils

import android.location.Location
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Helper class for location-based operations
 */
object LocationHelper {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculate distance between two coordinates using Haversine formula
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in meters
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (EARTH_RADIUS_METERS * c).toFloat()
    }

    /**
     * Check if user is within a certain distance of a location
     * @param userLat Current user latitude
     * @param userLon Current user longitude
     * @param targetLat Target location latitude
     * @param targetLon Target location longitude
     * @param radiusMeters Radius in meters (default 100m)
     * @return True if user is within radius
     */
    fun isWithinRadius(
        userLat: Double,
        userLon: Double,
        targetLat: Double,
        targetLon: Double,
        radiusMeters: Float = 100f
    ): Boolean {
        val distance = calculateDistance(userLat, userLon, targetLat, targetLon)
        return distance <= radiusMeters
    }

    /**
     * Convert Location to human-readable address approximation
     */
    fun formatCoordinates(latitude: Double, longitude: Double): String {
        return String.format("%.4f, %.4f", latitude, longitude)
    }
}

