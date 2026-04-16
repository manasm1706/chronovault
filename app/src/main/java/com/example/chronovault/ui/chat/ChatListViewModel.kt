package com.example.chronovault.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.data.repository.ChatRepository
import com.example.chronovault.data.repository.NotificationRepository
import com.example.chronovault.data.repository.UserRepository
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.utils.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatListViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        const val TAG_CHAT = "CHAT"
    }

    private val chatRepository: ChatRepository = ServiceLocator.provideChatRepository(application)
    private val notificationRepository: NotificationRepository = ServiceLocator.provideNotificationRepository(application)
    private val userRepository: UserRepository = ServiceLocator.provideUserRepository(application)

    private val _chats = MutableLiveData<List<ChatSummary>>(emptyList())
    val chats: LiveData<List<ChatSummary>> = _chats

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _filteredChats = MediatorLiveData<List<ChatSummary>>(emptyList())
    val filteredChats: LiveData<List<ChatSummary>> = _filteredChats

    private val _loadingState = MutableLiveData<LoadingState>(LoadingState.Idle)
    val loadingState: LiveData<LoadingState> = _loadingState

    private val _openChatEvent = MutableLiveData<ChatSummary?>(null)
    val openChatEvent: LiveData<ChatSummary?> = _openChatEvent

    private val knownLastTimestamps = mutableMapOf<String, Long>()
    private var initialized = false

    init {
        _filteredChats.addSource(_chats) { applyFilter() }
        _filteredChats.addSource(_searchQuery) { applyFilter() }
        observeChats()
    }

    fun getCurrentUserId(): String = chatRepository.getCurrentUserId().orEmpty()

    private fun observeChats() {
        _loadingState.value = LoadingState.Loading
        viewModelScope.launch {
            try {
                chatRepository.observeChats().collectLatest { summaries ->
                    // Just set the chats directly without waiting for display names
                    _chats.value = summaries
                    applyFilter()
                    _loadingState.value = LoadingState.Success
                    handleIncomingMessageNotifications(summaries)
                }
            } catch (e: Exception) {
                val fsError = e as? FirebaseFirestoreException
                val message = if (fsError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    "Chat sync failed. Check permissions."
                } else {
                    e.message ?: "Failed to load chats"
                }
                Log.e(TAG_CHAT, "Failed to observe chats", e)
                _loadingState.value = LoadingState.Error(message)
            }
        }
    }

    private suspend fun resolveDisplayNames(summaries: List<ChatSummary>): List<ChatSummary> {
        val userIds = buildSet {
            summaries.forEach { add(it.otherUserId) }
            summaries.forEach { add(it.lastSenderId) }
        }.toList()

        val profiles = userRepository.getUsersByIds(userIds).getOrDefault(emptyList())
            .associateBy { (it["id"] as? String).orEmpty() }

        return summaries.map { chat ->
            val otherProfile = profiles[chat.otherUserId]
            val senderProfile = profiles[chat.lastSenderId]
            chat.copy(
                otherUserName = (otherProfile?.get("name") as? String).orEmpty().ifBlank { chat.otherUserId },
                otherUserEmail = (otherProfile?.get("email") as? String).orEmpty(),
                lastSenderName = (senderProfile?.get("name") as? String).orEmpty().ifBlank { chat.lastSenderId }
            )
        }
    }

    private suspend fun handleIncomingMessageNotifications(summaries: List<ChatSummary>) {
        val currentUserId = getCurrentUserId()
        if (currentUserId.isBlank()) return

        if (!initialized) {
            summaries.forEach { knownLastTimestamps[it.chatId] = it.lastTimestamp }
            initialized = true
            return
        }

        summaries.forEach { chat ->
            val known = knownLastTimestamps[chat.chatId] ?: 0L
            if (chat.lastTimestamp > known && chat.lastSenderId.isNotBlank() && chat.lastSenderId != currentUserId) {
                if (ChatSessionManager.activeChatId == chat.chatId) {
                    knownLastTimestamps[chat.chatId] = chat.lastTimestamp
                    return@forEach
                }

                notificationRepository.createChatMessageNotification(
                    chatId = chat.chatId,
                    senderId = chat.lastSenderId,
                    preview = chat.lastMessage
                )
                NotificationHelper.sendChatMessageNotification(
                    context = getApplication(),
                    chatId = chat.chatId,
                    otherUserId = chat.lastSenderId,
                    senderName = chat.lastSenderName.ifBlank { chat.lastSenderId },
                    message = chat.lastMessage
                )
            }
            knownLastTimestamps[chat.chatId] = chat.lastTimestamp
        }
    }

    fun consumeOpenChatEvent() {
        _openChatEvent.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openChat(chat: ChatSummary) {
        _openChatEvent.value = chat
    }

    fun startChat(friendId: String) {
        if (friendId.isBlank()) return
        viewModelScope.launch {
            val currentUserId = getCurrentUserId()
            if (currentUserId.isBlank()) {
                _loadingState.value = LoadingState.Error("User not authenticated")
                return@launch
            }

            chatRepository.createOrGetChat(currentUserId, friendId)
                .onSuccess { chatId ->
                    _openChatEvent.value = ChatSummary(
                        chatId = chatId,
                        otherUserId = friendId,
                        otherUserName = friendId,
                        lastMessage = "",
                        lastTimestamp = 0L,
                        lastSenderId = "",
                        lastSenderName = ""
                    )
                }
                .onFailure { error ->
                    _loadingState.value = LoadingState.Error(error.message ?: "Failed to open chat")
                }
        }
    }

    fun openChatWithUser(userId: String) {
        startChat(userId)
    }

    private fun applyFilter() {
        val chats = _chats.value.orEmpty()
        val query = _searchQuery.value.orEmpty().trim()

        _filteredChats.value = if (query.isBlank()) {
            chats
        } else {
            chats.filter {
                it.otherUserId.contains(query, ignoreCase = true) ||
                    it.otherUserName.contains(query, ignoreCase = true) ||
                    it.lastSenderName.contains(query, ignoreCase = true) ||
                    it.lastMessage.contains(query, ignoreCase = true)
            }
        }
    }
}

