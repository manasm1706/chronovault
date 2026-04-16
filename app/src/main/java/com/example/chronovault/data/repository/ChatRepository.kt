package com.example.chronovault.data.repository

import com.example.chronovault.data.remote.firebase.FirebaseChatService
import com.example.chronovault.data.remote.firebase.FirebaseFriendService
import com.example.chronovault.ui.chat.ChatMessage
import com.example.chronovault.ui.chat.ChatMessageType
import com.example.chronovault.ui.chat.ChatSummary
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val firebaseChatService: FirebaseChatService,
    private val firebaseFriendService: FirebaseFriendService,
    private val preferencesManager: PreferencesManager
) {

    fun getCurrentUserId(): String? = preferencesManager.getUserId()

    fun observeChats(): Flow<List<ChatSummary>> {
        val userId = getCurrentUserId().orEmpty()
        return firebaseChatService.observeChats(userId)
    }

    fun observeLatestMessages(chatId: String): Flow<List<ChatMessage>> {
        return firebaseChatService.observeLatestMessages(chatId)
    }

    suspend fun loadOlderMessages(chatId: String, beforeTimestamp: Long): Result<List<ChatMessage>> {
        return firebaseChatService.loadOlderMessages(chatId, beforeTimestamp)
    }

    suspend fun sendTextMessage(otherUserId: String, text: String): Result<Unit> {
        val senderId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
        val senderName = preferencesManager.getUserName().orEmpty()
        val messageText = text.trim()
        if (messageText.isBlank()) return Result.failure(Exception("Message cannot be empty"))

        val friendCheck = firebaseFriendService.areFriends(senderId, otherUserId)
        if (friendCheck.isFailure || friendCheck.getOrDefault(false).not()) {
            return Result.failure(Exception("You can only chat with accepted friends"))
        }

        val chatId = createOrGetChat(senderId, otherUserId)
            .getOrElse { return Result.failure(it) }

        return firebaseChatService.sendMessage(
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            text = messageText,
            type = ChatMessageType.TEXT
        )
    }

    suspend fun sendCapsuleMessage(otherUserId: String, capsuleId: String, capsuleTitle: String): Result<Unit> {
        val senderId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
        val senderName = preferencesManager.getUserName().orEmpty()
        if (capsuleId.isBlank()) return Result.failure(Exception("Capsule missing"))

        val friendCheck = firebaseFriendService.areFriends(senderId, otherUserId)
        if (friendCheck.isFailure || friendCheck.getOrDefault(false).not()) {
            return Result.failure(Exception("You can only chat with accepted friends"))
        }

        val chatId = createOrGetChat(senderId, otherUserId)
            .getOrElse { return Result.failure(it) }

        return firebaseChatService.sendMessage(
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            text = capsuleTitle,
            type = ChatMessageType.CAPSULE,
            capsuleId = capsuleId
        )
    }

    suspend fun editMessage(chatId: String, messageId: String, newText: String): Result<Unit> {
        return firebaseChatService.editMessage(chatId, messageId, newText.trim())
    }

    suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> {
        return firebaseChatService.deleteMessage(chatId, messageId)
    }

    suspend fun ensureChatWithUser(otherUserId: String): Result<String> {
        val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
        if (otherUserId.isBlank()) return Result.failure(Exception("User is required"))
        return createOrGetChat(currentUserId, otherUserId)
    }

    suspend fun createOrGetChat(userA: String, userB: String): Result<String> {
        if (userA.isBlank() || userB.isBlank()) return Result.failure(Exception("Users are required"))
        return firebaseChatService.createOrGetChat(userA, userB)
    }

    fun buildChatIdForUsers(userA: String, userB: String): String {
        return firebaseChatService.buildChatId(userA, userB)
    }
}

