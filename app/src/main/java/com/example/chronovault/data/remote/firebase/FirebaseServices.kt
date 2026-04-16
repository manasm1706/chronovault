package com.example.chronovault.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * Firebase Authentication Service
 */
class FirebaseAuthService {

    private val firebaseAuth = FirebaseAuth.getInstance()

    suspend fun registerUser(email: String, password: String, name: String): Result<String> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: return Result.failure(Exception("User ID not found"))

            // Save user profile to Firestore
            FirebaseFirestore.getInstance().collection("users").document(userId).set(
                mapOf(
                    "email" to email,
                    "name" to name,
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<String> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: return Result.failure(Exception("User ID not found"))
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logoutUser() {
        firebaseAuth.signOut()
    }

    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    fun getCurrentUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}

/**
 * Firebase Firestore Service for Capsules
 */
class FirestoreCapsuleService {

    private val db = FirebaseFirestore.getInstance()
    private val capsuleCollection = "capsules"

    suspend fun createCapsule(userId: String, capsuleData: Map<String, Any>): Result<String> {
        return try {
            // FIX: 15
            val initialUnlocked = capsuleData["isUnlocked"] as? Boolean ?: false
            val providedId = (capsuleData["id"] as? String).orEmpty()
            val capsuleId = if (providedId.isNotBlank()) providedId else db.collection(capsuleCollection).document().id

            db.collection(capsuleCollection).document(capsuleId).set(
                capsuleData.toMutableMap().apply {
                    this["id"] = capsuleId
                    this["clientId"] = capsuleId
                    this["ownerId"] = userId
                    this["createdAt"] = this["createdAt"] ?: System.currentTimeMillis()
                    this["isUnlocked"] = initialUnlocked
                    this["isPublic"] = (capsuleData["isPublic"] as? Boolean) ?: false
                }
            ).await()

            Result.success(capsuleId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPublicCapsules(): Result<List<Map<String, Any>>> {
        return try {
            val docs = db.collection(capsuleCollection)
                .whereEqualTo("isPublic", true)
                .get()
                .await()
            Result.success(docs.documents.map { it.data?.plus("id" to it.id) ?: emptyMap() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserCapsules(userId: String): Result<List<Map<String, Any>>> {
        return try {
            val docs = db.collection(capsuleCollection)
                .whereEqualTo("ownerId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            Result.success(docs.documents.map { it.data?.plus("id" to it.id) ?: emptyMap() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCapsuleById(capsuleId: String): Result<Map<String, Any>> {
        return try {
            val doc = db.collection(capsuleCollection).document(capsuleId).get().await()
            Result.success(doc.data?.plus("id" to doc.id) ?: emptyMap())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCapsule(capsuleId: String, data: Map<String, Any>): Result<Unit> {
        return try {
            db.collection(capsuleCollection).document(capsuleId).update(
                data.toMutableMap().apply {
                    this["updatedAt"] = System.currentTimeMillis()
                }
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCapsule(capsuleId: String): Result<Unit> {
        return try {
            db.collection(capsuleCollection).document(capsuleId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlockCapsule(capsuleId: String): Result<Unit> {
        return try {
            db.collection(capsuleCollection).document(capsuleId).update(
                mapOf("isUnlocked" to true)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNearbyCapules(
        latitude: Double,
        longitude: Double,
        // FIX: 15
        radiusMeters: Float = 50f
    ): Result<List<Map<String, Any>>> {
        return try {
            // Get all capsules and filter by distance in app
            val docs = db.collection(capsuleCollection)
                .whereEqualTo("isLocationBased", true)
                .get()
                .await()

            val nearby = docs.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val capLat = data["latitude"] as? Double ?: return@mapNotNull null
                val capLon = data["longitude"] as? Double ?: return@mapNotNull null

                // Calculate distance using Haversine formula
                val distance = calculateDistance(latitude, longitude, capLat, capLon)
                if (distance <= radiusMeters) {
                    data.plus("id" to doc.id)
                } else null
            }

            Result.success(nearby)
        } catch (e: Exception) {
            Result.failure(e)
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
}

/**
 * Firebase Sharing Service for Capsule Permissions
 */
class FirebaseSharingService {

    private val db = FirebaseFirestore.getInstance()

    suspend fun shareCapsule(capsuleId: String, sharedWithUserId: String): Result<Unit> {
        return try {
            db.collection("capsules").document(capsuleId).update(
                "sharedWith", com.google.firebase.firestore.FieldValue.arrayUnion(sharedWithUserId)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeCapsuleSharing(capsuleId: String, userId: String): Result<Unit> {
        return try {
            db.collection("capsules").document(capsuleId).update(
                "sharedWith", com.google.firebase.firestore.FieldValue.arrayRemove(userId)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setCapsulePublicState(capsuleId: String, isPublic: Boolean): Result<Unit> {
        return try {
            db.collection("capsules").document(capsuleId).update("isPublic", isPublic).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSharedWithMeCapsules(userId: String): Result<List<Map<String, Any>>> {
        return try {
            val docs = db.collection("capsules")
                .whereArrayContains("sharedWith", userId)
                .get()
                .await()

            Result.success(docs.documents.map { it.data?.plus("id" to it.id) ?: emptyMap() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSharedWithMeCapsulesForIdentifiers(identifiers: List<String>): Result<List<Map<String, Any>>> {
        return try {
            val normalized = identifiers.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            if (normalized.isEmpty()) return Result.success(emptyList())

            val merged = linkedMapOf<String, Map<String, Any>>()
            normalized.forEach { identifier ->
                val docs = db.collection("capsules")
                    .whereArrayContains("sharedWith", identifier)
                    .get()
                    .await()
                docs.documents.forEach { doc ->
                    merged[doc.id] = doc.data?.plus("id" to doc.id) ?: emptyMap()
                }
            }

            Result.success(merged.values.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findRemoteCapsuleIdForOwner(
        ownerId: String,
        title: String,
        latitude: Double,
        longitude: Double,
        clientId: String
    ): Result<String?> {
        return try {
            val byClientId = db.collection("capsules")
                .whereEqualTo("ownerId", ownerId)
                .whereEqualTo("clientId", clientId)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.id
            if (!byClientId.isNullOrBlank()) return Result.success(byClientId)

            val byShape = db.collection("capsules")
                .whereEqualTo("ownerId", ownerId)
                .whereEqualTo("title", title)
                .whereEqualTo("latitude", latitude)
                .whereEqualTo("longitude", longitude)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.id

            Result.success(byShape)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeSharedWithMeCapsules(
        identifier: String,
        onUpdate: (List<Map<String, Any>>) -> Unit,
        onError: (FirebaseFirestoreException) -> Unit
    ): ListenerRegistration {
        return db.collection("capsules")
            .whereArrayContains("sharedWith", identifier)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val capsules = snapshot?.documents.orEmpty().map { doc ->
                    doc.data?.plus("id" to doc.id) ?: emptyMap()
                }
                onUpdate(capsules)
            }
    }
}

