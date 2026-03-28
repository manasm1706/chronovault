package com.example.chronovault.workers

import android.app.NotificationManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.utils.NotificationHelper

/**
 * TimeBasedUnlockWorker - Check for capsules that should unlock based on time
 * Runs periodically to check if any time-locked capsules are ready to unlock
 */
class TimeBasedUnlockWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val capsuleRepository = ServiceLocator.provideCapsuleRepository(applicationContext)
            val preferencesManager = ServiceLocator.providePreferencesManager(applicationContext)
            val notificationRepository = ServiceLocator.provideNotificationRepository(applicationContext)
            val userId = preferencesManager.getUserId() ?: return Result.retry()

            // Check for time-based unlocks
            val capsulesToUnlock = capsuleRepository.checkTimeBasedUnlocks(System.currentTimeMillis())

            capsulesToUnlock.forEach { capsule ->
                // Unlock the capsule
                capsuleRepository.unlockCapsule(capsule.id)

                // Send notification
                NotificationHelper.sendCapsuleUnlockedNotification(
                    applicationContext,
                    capsule.title,
                    "Your capsule \"${capsule.title}\" has been unlocked!"
                )

                notificationRepository.createUnlockNotification(
                    capsuleId = capsule.id,
                    capsuleTitle = capsule.title,
                    source = "time"
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "time_based_unlock_work"
        const val NOTIFICATION_ID = 1001
    }
}

