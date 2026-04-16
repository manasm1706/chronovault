package com.example.chronovault.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.data.local.entity.CapsuleEntity
import com.example.chronovault.data.repository.CapsuleRepository
import com.example.chronovault.data.repository.ChatRepository
import com.example.chronovault.ui.common.LoadingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val chatRepository: ChatRepository = ServiceLocator.provideChatRepository(application)
    private val capsuleRepository: CapsuleRepository = ServiceLocator.provideCapsuleRepository(application)

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _loadingState = MutableLiveData<LoadingState>(LoadingState.Idle)
    val loadingState: LiveData<LoadingState> = _loadingState

    private val _sendState = MutableLiveData<Result<Unit>?>(null)
    val sendState: LiveData<Result<Unit>?> = _sendState

    private val _messageActionState = MutableLiveData<Result<Unit>?>(null)
    val messageActionState: LiveData<Result<Unit>?> = _messageActionState

    private val _shareableCapsules = MutableLiveData<List<CapsuleEntity>>(emptyList())
    val shareableCapsules: LiveData<List<CapsuleEntity>> = _shareableCapsules

    private var observeJob: Job? = null
    private var activeChatId: String? = null
    private var activeOtherUserId: String? = null
    private var isLoadingMore = false
    private var oldestLoadedTimestamp: Long? = null

    fun getCurrentUserId(): String = chatRepository.getCurrentUserId().orEmpty()

    fun startChat(otherUserId: String, chatId: String? = null) {
        if (otherUserId.isBlank() && chatId.isNullOrBlank()) return
        val currentUserId = getCurrentUserId()
        if (currentUserId.isBlank()) return

        activeOtherUserId = otherUserId.ifBlank {
            chatId
                ?.split("_")
                ?.firstOrNull { it != currentUserId }
                .orEmpty()
        }
        val resolvedOtherUserId = activeOtherUserId.orEmpty()
        oldestLoadedTimestamp = null
        _messages.value = emptyList()

        observeJob?.cancel()
        _loadingState.value = LoadingState.Loading
        observeJob = viewModelScope.launch {
            try {
                activeChatId = when {
                    chatId.isNullOrBlank() -> {
                        chatRepository.createOrGetChat(currentUserId, resolvedOtherUserId)
                            .getOrElse { throw it }
                    }
                    resolvedOtherUserId.isNotBlank() -> {
                        chatRepository.createOrGetChat(currentUserId, resolvedOtherUserId)
                            .getOrElse { throw it }
                    }
                    else -> chatId
                }

                chatRepository.observeLatestMessages(activeChatId!!).collectLatest { latest ->
                    val merged = (latest + _messages.value.orEmpty())
                        .distinctBy { it.messageId }
                        .sortedBy { it.timestamp }
                    _messages.value = merged
                    oldestLoadedTimestamp = merged.firstOrNull()?.timestamp
                    _loadingState.value = LoadingState.Success
                }
            } catch (_: CancellationException) {
                // ViewModel lifecycle cancellation is expected.
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "Failed to open chat")
            }
        }
    }

    fun sendText(text: String) {
        val target = activeOtherUserId ?: return
        viewModelScope.launch {
            _sendState.value = chatRepository.sendTextMessage(target, text)
        }
    }

    fun sendCapsule(capsuleId: String, capsuleTitle: String) {
        val target = activeOtherUserId ?: return
        viewModelScope.launch {
            _sendState.value = chatRepository.sendCapsuleMessage(target, capsuleId, capsuleTitle)
        }
    }

    fun editMessage(messageId: String, newText: String) {
        val chatId = activeChatId ?: return
        if (messageId.isBlank() || newText.isBlank()) return
        viewModelScope.launch {
            _messageActionState.value = chatRepository.editMessage(chatId, messageId, newText)
        }
    }

    fun deleteMessage(messageId: String) {
        val chatId = activeChatId ?: return
        if (messageId.isBlank()) return
        viewModelScope.launch {
            _messageActionState.value = chatRepository.deleteMessage(chatId, messageId)
        }
    }

    fun loadShareableCapsules() {
        val userId = getCurrentUserId()
        if (userId.isBlank()) return
        viewModelScope.launch {
            val capsules = runCatching {
                capsuleRepository.getUserCapsules(userId).first()
            }.getOrDefault(emptyList())
            _shareableCapsules.value = capsules
        }
    }

    fun loadMore() {
        val chatId = activeChatId ?: return
        val oldest = oldestLoadedTimestamp ?: _messages.value.orEmpty().firstOrNull()?.timestamp ?: return
        if (isLoadingMore) return
        isLoadingMore = true
        viewModelScope.launch {
            val older = chatRepository.loadOlderMessages(chatId, oldest).getOrDefault(emptyList())
            if (older.isNotEmpty()) {
                val merged = (older + _messages.value.orEmpty())
                    .distinctBy { it.messageId }
                    .sortedBy { it.timestamp }
                _messages.value = merged
                oldestLoadedTimestamp = merged.firstOrNull()?.timestamp
            }
            isLoadingMore = false
        }
    }

    fun stopChat() {
        observeJob?.cancel()
        observeJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopChat()
    }
}


