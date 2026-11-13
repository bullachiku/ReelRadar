package com.reelradar.app.domain.repository

import com.reelradar.app.core.util.Resource
import com.reelradar.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun signInWithEmail(email: String, password: String): Flow<Resource<User>>
    fun signUpWithEmail(email: String, password: String, username: String): Flow<Resource<User>>
    fun signInWithGoogle(idToken: String): Flow<Resource<User>>
    fun sendPasswordReset(email: String): Flow<Resource<Unit>>
    suspend fun signOut()
    fun deleteAccount(): Flow<Resource<Unit>>
    fun getCurrentUser(): User?
    val currentUserFlow: Flow<User?>
}
