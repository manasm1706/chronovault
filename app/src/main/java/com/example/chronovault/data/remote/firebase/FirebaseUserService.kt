package com.example.chronovault.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import kotlinx.coroutines.tasks.await

/**
 * Firebase User Profile Service
 * Handles user profile data operations
 */
class FirebaseUserService {

    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = "users"

    suspend fun getUserProfile(userId: String): Result<Map<String, Any>> {
        return try {
            val doc = db.collection(usersCollection).document(userId).get().await()
            Result.success(doc.data?.plus("id" to doc.id) ?: emptyMap())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(userId: String, data: Map<String, Any>): Result<Unit> {
        return try {
            db.collection(usersCollection).document(userId).update(
                data.toMutableMap().apply {
                    this["updatedAt"] = System.currentTimeMillis()
                }
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserByEmail(email: String): Result<Map<String, Any>> {
        return try {
            val docs = db.collection(usersCollection)
                .whereEqualTo("email", email)
                .get()
                .await()

            if (docs.documents.isNotEmpty()) {
                Result.success(docs.documents[0].data?.plus("id" to docs.documents[0].id) ?: emptyMap())
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<List<Map<String, Any>>> {
        return try {
            val normalized = query.trim()
            if (normalized.isBlank()) return Result.success(emptyList())

            val docs = db.collection(usersCollection)
                .whereGreaterThanOrEqualTo("name", normalized)
                .whereLessThanOrEqualTo("name", normalized + "\uf8ff")
                .get()
                .await()

            Result.success(docs.documents.map { it.data?.plus("id" to it.id) ?: emptyMap() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUsersByIds(userIds: List<String>): Result<List<Map<String, Any>>> {
        return try {
            val normalized = userIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            if (normalized.isEmpty()) return Result.success(emptyList())

            val docs = normalized
                .chunked(10)
                .flatMap { chunk ->
                    db.collection(usersCollection)
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .await()
                        .documents
                }

            val merged = linkedMapOf<String, Map<String, Any>>()
            docs.forEach { doc ->
                merged[doc.id] = doc.data?.plus("id" to doc.id) ?: emptyMap()
            }

            Result.success(merged.values.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            db.collection(usersCollection).document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

