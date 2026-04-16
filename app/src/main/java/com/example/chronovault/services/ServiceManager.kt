package com.example.chronovault.services

/**
 * Global guard for long-running app services.
 */
object ServiceManager {
    @Volatile
    var isLocationServiceRunning: Boolean = false
}

