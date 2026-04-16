package com.example.chronovault.services

import android.util.Log
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives FCM push payloads and routes them through NotificationHelper.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to save FCM token", error)
            }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        NotificationHelper.createChannel(this)

        val data = remoteMessage.data
        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: "ChronoVault"
        val body = remoteMessage.notification?.body
            ?: data["message"]
            ?: "You have a new update"
        val type = data["type"].orEmpty()

        when (type) {
            "unlock", "location_unlock" -> {
                NotificationHelper.sendCapsuleUnlockedNotification(this, title, body)
            }

            "shared" -> {
                NotificationHelper.sendSharedNotification(this, title, body)
                val capsuleId = data["capsuleId"]
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
                        ServiceLocator.provideNotificationRepository(applicationContext)
                            .createSharedNotification(capsuleId, title)
                    }.onFailure { error ->
                        Log.w(TAG, "Failed to persist shared push in Room", error)
                    }
                }
            }

            "nearby" -> NotificationHelper.sendNearbyCapsuleAlert(this)
            "created" -> NotificationHelper.sendCapsuleCreatedNotification(this)
            "discovered" -> NotificationHelper.sendMemoryDiscoveredNotification(this)
            else -> NotificationHelper.showNotification(this, title, body)
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}

