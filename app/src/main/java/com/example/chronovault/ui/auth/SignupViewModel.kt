package com.example.chronovault.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chronovault.data.ServiceLocator
import com.example.chronovault.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Signup screen
 */
class SignupViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository = ServiceLocator.provideAuthRepository(application)

    private val _signupState = MutableLiveData<SignupState>(SignupState.Idle)
    val signupState: LiveData<SignupState> = _signupState

    private val _name = MutableLiveData("")
    val name: LiveData<String> = _name

    private val _email = MutableLiveData("")
    val email: LiveData<String> = _email

    private val _password = MutableLiveData("")
    val password: LiveData<String> = _password

    private val _confirmPassword = MutableLiveData("")
    val confirmPassword: LiveData<String> = _confirmPassword

    fun setName(newName: String) {
        _name.value = newName
    }

    fun setEmail(newEmail: String) {
        _email.value = newEmail
    }

    fun setPassword(newPassword: String) {
        _password.value = newPassword
    }

    fun setConfirmPassword(newConfirmPassword: String) {
        _confirmPassword.value = newConfirmPassword
    }

    fun signup() {
        val name = _name.value.orEmpty().trim()
        val email = _email.value.orEmpty().trim()
        val password = _password.value.orEmpty()
        val confirmPassword = _confirmPassword.value.orEmpty()

        // Validation
        if (name.isEmpty()) {
            _signupState.value = SignupState.Error("Name is required")
            return
        }

        if (name.length < 2) {
            _signupState.value = SignupState.Error("Name must be at least 2 characters")
            return
        }

        if (email.isEmpty()) {
            _signupState.value = SignupState.Error("Email is required")
            return
        }

        if (!email.isValidEmail()) {
            _signupState.value = SignupState.Error("Invalid email format")
            return
        }

        if (password.isEmpty()) {
            _signupState.value = SignupState.Error("Password is required")
            return
        }

        if (!password.isValidPassword()) {
            _signupState.value = SignupState.Error("Password must be at least 6 characters")
            return
        }

        if (password != confirmPassword) {
            _signupState.value = SignupState.Error("Passwords do not match")
            return
        }

        _signupState.value = SignupState.Loading

        viewModelScope.launch {
            authRepository.registerUser(email, password, name).collect { result ->
                result.onSuccess { userId ->
                    _signupState.value = SignupState.Success(userId)
                }
                result.onFailure { exception ->
                    _signupState.value = SignupState.Error(exception.message ?: "Signup failed")
                }
            }
        }
    }

    private fun String.isValidEmail(): Boolean {
        return this.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))
    }

    private fun String.isValidPassword(): Boolean {
        return this.length >= 6
    }
}

sealed class SignupState {
    object Idle : SignupState()
    object Loading : SignupState()
    data class Success(val userId: String) : SignupState()
    data class Error(val message: String) : SignupState()
}

