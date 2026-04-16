package com.example.chronovault.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.launch

/**
 * ViewModel for Login screen
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository = ServiceLocator.provideAuthRepository(application)

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    private val _email = MutableLiveData("")
    val email: LiveData<String> = _email

    private val _password = MutableLiveData("")
    val password: LiveData<String> = _password

    private val _resetPasswordState = MutableLiveData<ResetPasswordState>(ResetPasswordState.Idle)
    val resetPasswordState: LiveData<ResetPasswordState> = _resetPasswordState

    fun setEmail(newEmail: String) {
        _email.value = newEmail
    }

    fun setPassword(newPassword: String) {
        _password.value = newPassword
    }

    fun login() {
        val email = _email.value.orEmpty().trim()
        val password = _password.value.orEmpty()

        // Validation
        if (email.isEmpty()) {
            _loginState.value = LoginState.Error("Email is required")
            return
        }

        if (!email.isValidEmail()) {
            _loginState.value = LoginState.Error("Invalid email format")
            return
        }

        if (password.isEmpty()) {
            _loginState.value = LoginState.Error("Password is required")
            return
        }

        if (!password.isValidPassword()) {
            _loginState.value = LoginState.Error("Password must be at least 6 characters")
            return
        }

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            authRepository.loginUser(email, password).collect { result ->
                result.onSuccess { userId ->
                    _loginState.value = LoginState.Success(userId)
                }
                result.onFailure { exception ->
                    _loginState.value = LoginState.Error(mapAuthError(exception))
                }
            }
        }
    }

    fun sendPasswordResetEmail(emailInput: String) {
        val email = emailInput.trim()
        if (email.isEmpty()) {
            _resetPasswordState.value = ResetPasswordState.Error("Email is required")
            return
        }
        if (!email.isValidEmail()) {
            _resetPasswordState.value = ResetPasswordState.Error("Please enter a valid email address")
            return
        }

        _resetPasswordState.value = ResetPasswordState.Loading
        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(email)
                .onSuccess {
                    _resetPasswordState.value =
                        ResetPasswordState.Success("Password reset link sent to your email")
                }
                .onFailure { exception ->
                    _resetPasswordState.value = ResetPasswordState.Error(mapResetError(exception))
                }
        }
    }

    fun resetPasswordResetState() {
        _resetPasswordState.value = ResetPasswordState.Idle
    }

    private fun String.isValidEmail(): Boolean {
        return this.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))
    }

    private fun String.isValidPassword(): Boolean {
        return this.length >= 6
    }

    private fun mapAuthError(exception: Throwable): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> "No account found for this email"
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
            else -> exception.message ?: "Login failed"
        }
    }

    private fun mapResetError(exception: Throwable): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> "No account found for this email"
            is FirebaseAuthInvalidCredentialsException -> "Please enter a valid email address"
            else -> exception.message ?: "Failed to send reset link"
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val userId: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

sealed class ResetPasswordState {
    object Idle : ResetPasswordState()
    object Loading : ResetPasswordState()
    data class Success(val message: String) : ResetPasswordState()
    data class Error(val message: String) : ResetPasswordState()
}

