package com.reelradar.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelradar.app.core.util.Resource
import com.reelradar.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUserFlow.collect { user ->
                _state.update { it.copy(user = user) }
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _state.update { it.copy(error = "Please fill in all fields") }
            return
        }
        viewModelScope.launch {
            authRepository.signInWithEmail(email.trim(), pass).collect { resource ->
                handleAuthResult(resource)
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, username: String) {
        if (email.isBlank() || pass.isBlank() || username.isBlank()) {
            _state.update { it.copy(error = "All fields are required") }
            return
        }
        if (pass.length < 6) {
            _state.update { it.copy(error = "Password must be at least 6 characters") }
            return
        }
        viewModelScope.launch {
            authRepository.signUpWithEmail(email.trim(), pass, username.trim()).collect { resource ->
                handleAuthResult(resource)
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken).collect { resource ->
                handleAuthResult(resource)
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _state.update { it.copy(error = "Enter your email address") }
            return
        }
        viewModelScope.launch {
            authRepository.sendPasswordReset(email.trim()).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _state.update { it.copy(isLoading = true, error = null) }
                    is Resource.Success -> _state.update { it.copy(isLoading = false, isPasswordResetSent = true) }
                    is Resource.Error -> _state.update { it.copy(isLoading = false, error = resource.message) }
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _state.update { AuthState() }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.deleteAccount().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _state.update { it.copy(isLoading = true) }
                    is Resource.Success -> {
                        _state.update { AuthState() }
                        onSuccess()
                    }
                    is Resource.Error -> _state.update { it.copy(isLoading = false, error = resource.message) }
                }
            }
        }
    }

    private fun handleAuthResult(resource: Resource<com.reelradar.app.domain.model.User>) {
        when (resource) {
            is Resource.Loading -> _state.update { it.copy(isLoading = true, error = null) }
            is Resource.Success -> _state.update {
                it.copy(isLoading = false, user = resource.data, isSuccess = true, error = null)
            }
            is Resource.Error -> _state.update {
                it.copy(isLoading = false, error = resource.message, isSuccess = false)
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
