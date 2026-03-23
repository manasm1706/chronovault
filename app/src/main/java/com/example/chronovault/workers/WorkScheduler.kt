package com.example.chronovault.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * WorkScheduler - Initialize and schedule all background workers
 */
object WorkScheduler {

    /**
     * Schedule all background workers
     * Call this from Application.onCreate()
     */
    fun scheduleAllWorkers(context: Context) {
        try {
            scheduleTimeBasedUnlockWorker(context)
        } catch (e: Exception) {
            android.util.Log.e("WorkScheduler", "Failed to schedule time-based worker", e)
        }

        try {
            scheduleLocationBasedUnlockWorker(context)
        } catch (e: Exception) {
            android.util.Log.e("WorkScheduler", "Failed to schedule location-based worker", e)
        }
    }

    /**
     * Schedule time-based unlock worker
     * Runs every 15 minutes to check for time-based unlocks
     */
    private fun scheduleTimeBasedUnlockWorker(context: Context) {
        try {
            val timeBasedWork = PeriodicWorkRequestBuilder<TimeBasedUnlockWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TimeBasedUnlockWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                timeBasedWork
            )
        } catch (e: Exception) {
            android.util.Log.w("WorkScheduler", "Time-based unlock worker not available")
        }
    }

    /**
     * Schedule location-based unlock worker
     * Runs every 30 minutes to check for nearby capsules
     */
    private fun scheduleLocationBasedUnlockWorker(context: Context) {
        try {
            val locationBasedWork = PeriodicWorkRequestBuilder<LocationBasedUnlockWorker>(
                30, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                LocationBasedUnlockWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                locationBasedWork
            )
        } catch (e: Exception) {
            android.util.Log.w("WorkScheduler", "Location-based unlock worker not available")
        }
    }

    /**
     * Cancel all scheduled workers
     */
    fun cancelAllWorkers(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TimeBasedUnlockWorker.WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(LocationBasedUnlockWorker.WORK_NAME)
    }
}

