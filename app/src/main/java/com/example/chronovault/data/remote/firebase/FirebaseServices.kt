package com.example.chronovault.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
            val docRef = db.collection(capsuleCollection).add(
                capsuleData.toMutableMap().apply {
                    this["ownerId"] = userId
                    this["createdAt"] = System.currentTimeMillis()
                    this["isUnlocked"] = false
                }
            ).await()
            Result.success(docRef.id)
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
        radiusMeters: Float = 100f
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
 * Firebase Storage Service for Images
 */
class FirebaseStorageService {

    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadImage(userId: String, imageBytes: ByteArray, fileName: String): Result<String> {
        return try {
            val ref = storage.reference.child("capsule_images/$userId/$fileName")
            ref.putBytes(imageBytes).await()
            val downloadUrl = ref.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

