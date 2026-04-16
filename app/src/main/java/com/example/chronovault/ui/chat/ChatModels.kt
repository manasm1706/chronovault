package com.example.chronovault.ui.chat

enum class ChatMessageType {
    TEXT,
    CAPSULE
}

data class ChatSummary(
    val chatId: String,
    val otherUserId: String,
    val otherUserName: String = "",
    val otherUserEmail: String = "",
    val lastMessage: String,
    val lastTimestamp: Long,
    val lastSenderId: String,
    val lastSenderName: String = ""
)

data class ChatMessage(
    val messageId: String,
    val senderId: String,
    val senderName: String = "",
    val text: String,
    val timestamp: Long,
    val type: ChatMessageType,
    val capsuleId: String? = null,
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false
)

