package com.example.chronovault.ui.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.data.repository.FriendRepository
import com.example.chronovault.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UserSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository = ServiceLocator.provideUserRepository(application)
    private val friendRepository: FriendRepository = ServiceLocator.provideFriendRepository(application)

    private val _users = MutableLiveData<List<UserSearchItem>>(emptyList())
    val users: LiveData<List<UserSearchItem>> = _users

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _friends = MutableLiveData<List<UserSearchItem>>(emptyList())

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private var searchJob: Job? = null

    init {
        observeFriends()
    }

    fun searchUsers(query: String) {
        val trimmed = query.trim()
        _searchQuery.value = trimmed
        searchJob?.cancel()

        if (trimmed.isBlank()) {
            _users.value = _friends.value.orEmpty()
            _loading.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(220L)
            _loading.value = true
            val currentUserId = userRepository.getUserId().orEmpty()

            val remoteMatches = userRepository.searchUsers(trimmed)
                .getOrDefault(emptyList())
                .mapNotNull { row ->
                    val id = (row["id"] as? String).orEmpty()
                    if (id.isBlank() || id == currentUserId) return@mapNotNull null
                    UserSearchItem(
                        id = id,
                        name = (row["name"] as? String).orEmpty().ifBlank { id },
                        email = (row["email"] as? String).orEmpty()
                    )
                }

            val fallbackFriends = _friends.value.orEmpty().filter {
                it.name.contains(trimmed, ignoreCase = true) ||
                    it.id.contains(trimmed, ignoreCase = true) ||
                    it.email.contains(trimmed, ignoreCase = true)
            }

            _users.value = if (remoteMatches.isNotEmpty()) remoteMatches else fallbackFriends
            _loading.value = false
        }
    }

    private fun observeFriends() {
        viewModelScope.launch {
            friendRepository.observeFriends().collectLatest { friends ->
                val friendIds = friends.map { it.friendUserId }.distinct()
                val profileById = userRepository.getUsersByIds(friendIds)
                    .getOrDefault(emptyList())
                    .associateBy { (it["id"] as? String).orEmpty() }

                val mapped = friendIds.map { friendId ->
                    val profile = profileById[friendId]
                    UserSearchItem(
                        id = friendId,
                        name = (profile?.get("name") as? String).orEmpty().ifBlank { friendId },
                        email = (profile?.get("email") as? String).orEmpty().ifBlank { "Friend" }
                    )
                }

                _friends.value = mapped
                if (_searchQuery.value.isNullOrBlank()) {
                    _users.value = mapped
                }
            }
        }
    }
}

