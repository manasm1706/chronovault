package com.example.chronovault.data.remote.firebase

import com.example.chronovault.ui.chat.ChatMessage
import com.example.chronovault.ui.chat.ChatMessageType
import com.example.chronovault.ui.chat.ChatSummary
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseChatService {

    private val db = FirebaseFirestore.getInstance()

    fun buildChatId(userA: String, userB: String): String {
        val sorted = listOf(userA, userB).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    suspend fun ensureChat(chatId: String, userA: String, userB: String): Result<Unit> {
        return try {
            val docRef = db.collection("chats").document(chatId)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                docRef.set(
                    mapOf(
                        "participants" to listOf(userA, userB).sorted(),
                        "lastMessage" to "",
                        "lastTimestamp" to 0L,
                        "lastSenderId" to ""
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeChats(userId: String): Flow<List<ChatSummary>> = callbackFlow {
        val registration = db.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("lastTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val chats = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val participants = (doc.get("participants") as? List<*>)?.mapNotNull { it as? String } ?: return@mapNotNull null
                    val otherUserId = participants.firstOrNull { it != userId } ?: return@mapNotNull null
                    ChatSummary(
                        chatId = doc.id,
                        otherUserId = otherUserId,
                        lastMessage = doc.getString("lastMessage").orEmpty(),
                        lastTimestamp = doc.getLong("lastTimestamp") ?: 0L,
                        lastSenderId = doc.getString("lastSenderId").orEmpty()
                    )
                }
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
        text: String,
        type: ChatMessageType,
        capsuleId: String? = null
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val messageData = mapOf(
                "senderId" to senderId,
                "text" to text,
                "timestamp" to now,
                "type" to type.name,
                "capsuleId" to capsuleId
            )

            db.collection("chats").document(chatId)
                .collection("messages")
                .add(messageData)
                .await()

            db.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessage" to text,
                    "lastTimestamp" to now,
                    "lastSenderId" to senderId
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapMessage(id: String, data: Map<String, Any>): ChatMessage? {
        val senderId = data["senderId"] as? String ?: return null
        val text = data["text"] as? String ?: ""
        val timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
        val type = (data["type"] as? String)?.let { runCatching { ChatMessageType.valueOf(it) }.getOrNull() } ?: ChatMessageType.TEXT
        val capsuleId = data["capsuleId"] as? String
        return ChatMessage(
            messageId = id,
            senderId = senderId,
            text = text,
            timestamp = timestamp,
            type = type,
            capsuleId = capsuleId
        )
    }
}

