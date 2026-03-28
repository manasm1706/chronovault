package com.example.chronovault.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
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
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )
            val doc = db.collection("friend_requests").add(payload).await()
            Result.success(doc.id)
        } catch (e: Exception) {
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

    suspend fun getFriendRequestsForUser(userId: String): Result<List<Map<String, Any>>> {
        return try {
            val docs = db.collection("friend_requests")
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()
            Result.success(docs.documents.mapNotNull { doc -> doc.data?.plus("id" to doc.id) })
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
}

data class FriendRequestRemote(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val status: String
)

