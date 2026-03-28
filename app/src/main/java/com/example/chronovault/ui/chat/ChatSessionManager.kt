package com.example.chronovault.ui.chat

/**
 * Tracks the currently open chat to suppress duplicate notifications while user is inside that chat.
 */
object ChatSessionManager {
    @Volatile
    var activeChatId: String? = null
}

