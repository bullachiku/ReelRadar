package com.reelradar.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.reelradar.app.core.util.Resource
import com.reelradar.app.data.local.datastore.PreferencesManager
import com.reelradar.app.data.remote.dto.UserDto
import com.reelradar.app.domain.model.User
import com.reelradar.app.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val preferencesManager: PreferencesManager
) : AuthRepository {

    override val currentUserFlow: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                trySend(
                    User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString()
                    )
                )
            } else {
                trySend(null)
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return User(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "",
            photoUrl = firebaseUser.photoUrl?.toString()
        )
    }

    override fun signInWithEmail(email: String, password: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User is null after sign in")
            
            // Fetch username from Firestore
            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            val username = userDoc.getString("username") ?: email.substringBefore("@")
            
            val user = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: email,
                username = username,
                displayName = firebaseUser.displayName ?: username,
                photoUrl = firebaseUser.photoUrl?.toString()
            )
            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to sign in"))
        }
    }

    override fun signUpWithEmail(
        email: String,
        password: String,
        username: String
    ): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            // Check if username is already taken
            val existing = firestore.collection("users")
                .whereEqualTo("username", username.trim().lowercase())
                .get()
                .await()
            if (!existing.isEmpty) {
                emit(Resource.Error("Username is already taken"))
                return@flow
            }

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User creation failed")

            val userDto = UserDto(
                uid = firebaseUser.uid,
                email = email,
                username = username.trim().lowercase(),
                displayName = username.trim(),
                createdAt = System.currentTimeMillis()
            )

            // Save user profile in Firestore
            firestore.collection("users").document(firebaseUser.uid).set(userDto).await()

            val user = User(
                uid = firebaseUser.uid,
                email = email,
                username = username.trim().lowercase(),
                displayName = username.trim()
            )
            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Sign up failed"))
        }
    }

    override fun signInWithGoogle(idToken: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Google sign in failed")

            val userDocRef = firestore.collection("users").document(firebaseUser.uid)
            val doc = userDocRef.get().await()

            val username = if (doc.exists() && doc.contains("username")) {
                doc.getString("username") ?: (firebaseUser.email?.substringBefore("@") ?: "user")
            } else {
                val defaultUsername = (firebaseUser.email?.substringBefore("@") ?: "user_${firebaseUser.uid.take(5)}")
                val userDto = UserDto(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    username = defaultUsername,
                    displayName = firebaseUser.displayName ?: defaultUsername,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    createdAt = System.currentTimeMillis()
                )
                userDocRef.set(userDto).await()
                defaultUsername
            }

            val user = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                username = username,
                displayName = firebaseUser.displayName ?: username,
                photoUrl = firebaseUser.photoUrl?.toString()
            )
            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Google sign in failed"))
        }
    }

    override fun sendPasswordReset(email: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            auth.sendPasswordResetEmail(email).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Password reset failed"))
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun deleteAccount(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                // Delete user firestore doc
                firestore.collection("users").document(uid).delete().await()
                // Delete user from Firebase Auth
                auth.currentUser?.delete()?.await()
                // Clear local preferences
                preferencesManager.clearAll()
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to delete account"))
        }
    }
}
