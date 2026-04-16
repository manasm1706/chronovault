package com.example.chronovault.data.remote.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Basic Firestore friend-request service.
 */
class FirebaseFriendService {

    private val db = FirebaseFirestore.getInstance()

    suspend fun sendFriendRequest(senderId: String, receiverId: String): Result<String> {
        return try {
            val payload = mapOf(
                "senderId" to senderId,
                "receiverId" to receiverId,
                "status" to "PENDING",
                "timestamp" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val doc = db.collection("friend_requests").add(payload).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Log.e("APP_ERROR", "friend_requests create failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    @Suppress("unused")
    suspend fun userExists(userId: String): Result<Boolean> {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            Result.success(doc.exists())
        } catch (e: Exception) {
            Log.e("APP_ERROR", "users read failed for id=$userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    @Suppress("unused")
    suspend fun hasPendingRequestBetween(userA: String, userB: String): Result<Boolean> {
        return try {
            val outgoing = db.collection("friend_requests")
                .whereEqualTo("senderId", userA)
                .whereEqualTo("receiverId", userB)
                .whereEqualTo("status", "PENDING")
                .limit(1)
                .get()
                .await()

            if (!outgoing.isEmpty) {
                return Result.success(true)
            }

            val incoming = db.collection("friend_requests")
                .whereEqualTo("senderId", userB)
                .whereEqualTo("receiverId", userA)
                .whereEqualTo("status", "PENDING")
                .limit(1)
                .get()
                .await()

            Result.success(!incoming.isEmpty)
        } catch (e: Exception) {
            Log.e("APP_ERROR", "friend_requests duplicate check failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            db.collection("friend_requests").document(requestId).update(
                mapOf(
                    "status" to "ACCEPTED",
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            db.collection("friend_requests").document(requestId).update(
                mapOf(
                    "status" to "REJECTED",
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFriendshipIfMissing(userA: String, userB: String): Result<Unit> {
        return try {
            val sorted = listOf(userA, userB).sorted()
            val friendId = "${sorted[0]}_${sorted[1]}"
            val docRef = db.collection("friends").document(friendId)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) return Result.success(Unit)

            docRef.set(
                mapOf(
                    "users" to sorted,
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun areFriends(userA: String, userB: String): Result<Boolean> {
        return try {
            val sorted = listOf(userA, userB).sorted()
            val friendId = "${sorted[0]}_${sorted[1]}"
            val snapshot = db.collection("friends").document(friendId).get().await()
            Result.success(snapshot.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeIncomingRequests(receiverId: String): Flow<List<FriendRequestRemote>> = callbackFlow {
        val registration = db.collection("friend_requests")
            .whereEqualTo("receiverId", receiverId)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val senderId = doc.getString("senderId") ?: return@mapNotNull null
                    val status = doc.getString("status") ?: "PENDING"
                    FriendRequestRemote(
                        id = doc.id,
                        senderId = senderId,
                        receiverId = receiverId,
                        status = status
                    )
                }
                trySend(requests)
            }

        awaitClose { registration.remove() }
    }

    fun observeAcceptedFriends(currentUserId: String): Flow<List<String>> = callbackFlow {
        Log.d("FIRESTORE_DEBUG", "Reading friends for user: $currentUserId")
        val registration = db.collection("friends")
            .whereArrayContains("users", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("APP_ERROR", "friends read failed for user=$currentUserId: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val friendIds = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val users = (doc.get("users") as? List<*>)?.mapNotNull { it as? String } ?: return@mapNotNull null
                    users.firstOrNull { it != currentUserId }
                }
                trySend(friendIds)
            }

        awaitClose { registration.remove() }
    }
}

data class FriendRequestRemote(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val status: String
)

