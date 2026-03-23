package com.example.chronovault.ui.common

/**
 * Shared UI state classes used across ViewModels
 */
sealed class LoadingState {
    object Idle : LoadingState()
    object Loading : LoadingState()
    object Success : LoadingState()
    data class Error(val message: String) : LoadingState()
}

