package com.example.chronovault.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.data.local.CommentDao
import com.example.chronovault.data.local.OwnerCapsuleCommentItem
import com.example.chronovault.utils.PreferencesManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyCapsuleCommentsViewModel(application: Application) : AndroidViewModel(application) {

    private val commentDao: CommentDao = ServiceLocator.provideCommentDao(application)
    private val preferencesManager: PreferencesManager = ServiceLocator.providePreferencesManager(application)

    private val _comments = MutableLiveData<List<OwnerCapsuleCommentItem>>(emptyList())
    val comments: LiveData<List<OwnerCapsuleCommentItem>> = _comments

    init {
        observeOwnerComments()
    }

    private fun observeOwnerComments() {
        val userId = preferencesManager.getUserId().orEmpty()
        if (userId.isBlank()) {
            _comments.value = emptyList()
            return
        }

        viewModelScope.launch {
            commentDao.getCommentsForCapsuleOwner(userId).collectLatest { rows ->
                _comments.value = rows
            }
        }
    }
}

