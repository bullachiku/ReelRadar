package com.reelradar.app.presentation.auth

import com.reelradar.app.domain.model.User

data class AuthState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isPasswordResetSent: Boolean = false
)
