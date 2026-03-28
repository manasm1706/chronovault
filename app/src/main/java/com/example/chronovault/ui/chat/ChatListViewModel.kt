package com.example.chronovault.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.data.repository.ChatRepository
import com.example.chronovault.data.repository.NotificationRepository
import com.example.chronovault.ui.common.LoadingState
import com.example.chronovault.utils.NotificationHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatListViewModel(application: Application) : AndroidViewModel(application) {

    private val chatRepository: ChatRepository = ServiceLocator.provideChatRepository(application)
    private val notificationRepository: NotificationRepository = ServiceLocator.provideNotificationRepository(application)

    private val _chats = MutableLiveData<List<ChatSummary>>(emptyList())
    val chats: LiveData<List<ChatSummary>> = _chats

    private val _loadingState = MutableLiveData<LoadingState>(LoadingState.Idle)
    val loadingState: LiveData<LoadingState> = _loadingState

    private val _openChatEvent = MutableLiveData<ChatSummary?>(null)
    val openChatEvent: LiveData<ChatSummary?> = _openChatEvent

    private val knownLastTimestamps = mutableMapOf<String, Long>()
    private var initialized = false

    init {
        observeChats()
    }

    fun getCurrentUserId(): String = chatRepository.getCurrentUserId().orEmpty()

    private fun observeChats() {
        _loadingState.value = LoadingState.Loading
        viewModelScope.launch {
            chatRepository.observeChats().collectLatest { summaries ->
                _chats.value = summaries
                _loadingState.value = LoadingState.Success
                handleIncomingMessageNotifications(summaries)
            }
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
                    senderName = chat.lastSenderId,
                    message = chat.lastMessage
                )
            }
            knownLastTimestamps[chat.chatId] = chat.lastTimestamp
        }
    }

    fun consumeOpenChatEvent() {
        _openChatEvent.value = null
    }

    fun openChat(chat: ChatSummary) {
        _openChatEvent.value = chat
    }
}

