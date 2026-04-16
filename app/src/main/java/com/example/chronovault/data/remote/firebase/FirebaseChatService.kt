package com.example.chronovault.data.remote.firebase

import android.util.Log
import com.example.chronovault.ui.chat.ChatMessage
import com.example.chronovault.ui.chat.ChatMessageType
import com.example.chronovault.ui.chat.ChatSummary
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseChatService {

    private val db = FirebaseFirestore.getInstance()
    private companion object {
        const val TAG_CHAT = "CHAT"
        const val TAG_CHAT_DEBUG = "CHAT_DEBUG"
    }

    fun buildChatId(userA: String, userB: String): String {
        val sorted = listOf(userA, userB).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    suspend fun createOrGetChat(userA: String, userB: String): Result<String> {
        return try {
            val chatId = buildChatId(userA, userB)
            val chatRef = db.collection("chats").document(chatId)

            val createPayload = mapOf(
                "participants" to listOf(userA, userB).sorted(),
                "createdAt" to FieldValue.serverTimestamp(),
                "lastMessage" to "",
                "lastMessageTimestamp" to 0L,
                "lastTimestamp" to 0L,
                "lastSenderId" to ""
            )

            // Some rule sets deny get() for non-existing chat docs. In that case,
            // perform a safe merge-set create path without requiring an initial read.
            val doc = runCatching { chatRef.get().await() }.getOrNull()
            if (doc == null) {
                chatRef.set(createPayload, SetOptions.merge()).await()
            } else if (!doc.exists()) {
                chatRef.set(createPayload).await()
            }

            Result.success(chatId)
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return try {
                    val chatId = buildChatId(userA, userB)
                    db.collection("chats").document(chatId)
                        .set(
                            mapOf(
                                "participants" to listOf(userA, userB).sorted(),
                                "createdAt" to FieldValue.serverTimestamp(),
                                "lastMessage" to "",
                                "lastMessageTimestamp" to 0L,
                                "lastTimestamp" to 0L,
                                "lastSenderId" to ""
                            ),
                            SetOptions.merge()
                        ).await()
                    Result.success(chatId)
                } catch (fallback: Exception) {
                    Result.failure(fallback)
                }
            }
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeChats(userId: String): Flow<List<ChatSummary>> = callbackFlow {
        Log.d(TAG_CHAT_DEBUG, "Current UID: $userId")
        var lastEmittedChats: List<ChatSummary> = emptyList()

        val registration = db.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG_CHAT, "Listener error", error)
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        close(error)
                    }
                    // Do not clear existing list on listener errors.
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.d(TAG_CHAT, "Snapshot is null, keeping old list")
                    return@addSnapshotListener
                }

                if (snapshot.isEmpty && lastEmittedChats.isNotEmpty()) {
                    Log.d(TAG_CHAT, "No chats found, keeping old list")
                    return@addSnapshotListener
                }

                val chats = snapshot.documents.mapNotNull { doc ->
                    val participants = (doc.get("participants") as? List<*>)?.mapNotNull { it as? String } ?: return@mapNotNull null
                    Log.d(TAG_CHAT_DEBUG, "Participants: $participants")
                    val otherUserId = participants.firstOrNull { it != userId } ?: return@mapNotNull null
                    ChatSummary(
                        chatId = doc.id,
                        otherUserId = otherUserId,
                        lastMessage = doc.getString("lastMessage").orEmpty(),
                        lastTimestamp = doc.getLong("lastMessageTimestamp") ?: doc.getLong("lastTimestamp") ?: 0L,
                        lastSenderId = doc.getString("lastSenderId").orEmpty()
                    )
                }
                lastEmittedChats = chats
                trySend(chats)
            }
        awaitClose { registration.remove() }
    }

    fun observeLatestMessages(chatId: String, pageSize: Long = 20L): Flow<List<ChatMessage>> = callbackFlow {
        val registration = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(pageSize)
            .addSnapshotListener { snapshot, _ ->
                val messages = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    mapMessage(doc.id, doc.data ?: return@mapNotNull null)
                }.sortedBy { it.timestamp }
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    suspend fun loadOlderMessages(chatId: String, beforeTimestamp: Long, pageSize: Long = 20L): Result<List<ChatMessage>> {
        return try {
            val docs = db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(beforeTimestamp)
                .limit(pageSize)
                .get()
                .await()

            val messages = docs.documents.mapNotNull { doc -> mapMessage(doc.id, doc.data ?: return@mapNotNull null) }
                .sortedBy { it.timestamp }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String = "",
        text: String,
        type: ChatMessageType,
        capsuleId: String? = null
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            Log.d(TAG_CHAT_DEBUG, "Sending message chatId=$chatId sender=$senderId type=$type")
            val messageData = mapOf(
                "senderId" to senderId,
                "senderName" to senderName,
                "text" to text,
                "timestamp" to now,
                "type" to type.name,
                "capsuleId" to capsuleId,
                "isDeleted" to false,
                "isEdited" to false
            )

            db.collection("chats").document(chatId)
                .collection("messages")
                .add(messageData)
                .await()

            db.collection("chats").document(chatId)
                .set(
                    mapOf(
                        "lastMessage" to text,
                        "lastMessageTimestamp" to now,
                        "lastTimestamp" to now,
                        "lastSenderId" to senderId
                    ),
                    SetOptions.merge()
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editMessage(chatId: String, messageId: String, newText: String): Result<Unit> {
        return try {
            db.collection("chats").document(chatId)
                .collection("messages")
                .document(messageId)
                .update(
                    mapOf(
                        "text" to newText,
                        "isEdited" to true,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> {
        return try {
            db.collection("chats").document(chatId)
                .collection("messages")
                .document(messageId)
                .update(
                    mapOf(
                        "text" to "This message was deleted",
                        "isDeleted" to true,
                        "isEdited" to false,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapMessage(id: String, data: Map<String, Any>): ChatMessage? {
        val senderId = data["senderId"] as? String ?: return null
        val senderName = data["senderName"] as? String ?: ""
        val text = data["text"] as? String ?: ""
        val timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
        val type = (data["type"] as? String)?.let { runCatching { ChatMessageType.valueOf(it) }.getOrNull() } ?: ChatMessageType.TEXT
        val capsuleId = data["capsuleId"] as? String
        val isDeleted = data["isDeleted"] as? Boolean ?: false
        val isEdited = data["isEdited"] as? Boolean ?: false
        return ChatMessage(
            messageId = id,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestamp = timestamp,
            type = type,
            capsuleId = capsuleId,
            isDeleted = isDeleted,
            isEdited = isEdited
        )
    }
}

