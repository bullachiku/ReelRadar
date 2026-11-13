package com.reelradar.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.reelradar.app.core.util.Resource
import com.reelradar.app.data.remote.dto.FriendshipDto
import com.reelradar.app.domain.model.FriendUser
import com.reelradar.app.domain.model.User
import com.reelradar.app.domain.repository.FriendsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendsRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : FriendsRepository {

    override fun searchUserByUsername(username: String): Flow<Resource<User?>> = flow {
        emit(Resource.Loading())
        try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("username", username.trim().lowercase())
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                emit(Resource.Success(null))
            } else {
                val doc = querySnapshot.documents[0]
                val user = User(
                    uid = doc.id,
                    email = doc.getString("email") ?: "",
                    username = doc.getString("username") ?: "",
                    displayName = doc.getString("displayName") ?: (doc.getString("username") ?: "")
                )
                emit(Resource.Success(user))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to find user"))
        }
    }

    override fun sendFriendRequest(targetUser: User): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val currentUid = auth.currentUser?.uid ?: throw Exception("Not signed in")
            if (currentUid == targetUser.uid) {
                emit(Resource.Error("You cannot add yourself as a friend"))
                return@flow
            }

            // Consistent sorted friendship doc id
            val friendshipId = if (currentUid < targetUser.uid) "${currentUid}_${targetUser.uid}" else "${targetUser.uid}_${currentUid}"

            val currentDoc = firestore.collection("users").document(currentUid).get().await()
            val currentUsername = currentDoc.getString("username") ?: "User"

            val friendship = FriendshipDto(
                id = friendshipId,
                uids = listOf(currentUid, targetUser.uid),
                status = "accepted", // Auto-accepted for simplified mutual tracking
                user1Username = currentUsername,
                user2Username = targetUser.username,
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection("friendships").document(friendshipId).set(friendship).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to add friend"))
        }
    }

    override fun getLeaderboard(todayDate: String): Flow<List<FriendUser>> = callbackFlow {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("friendships")
            .whereArrayContains("uids", currentUid)
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Process friend daily counts asynchronously
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val friendsList = mutableListOf<FriendUser>()

                    // First add current user to leaderboard
                    val myDaily = firestore.collection("users")
                        .document(currentUid)
                        .collection("daily")
                        .document(todayDate)
                        .get()
                        .await()
                    val myCount = myDaily.getLong("count")?.toInt() ?: 0
                    val myUserDoc = firestore.collection("users").document(currentUid).get().await()
                    friendsList.add(
                        FriendUser(
                            uid = currentUid,
                            username = myUserDoc.getString("username") ?: "You",
                            displayName = "You",
                            todayCount = myCount
                        )
                    )

                    // Fetch friends' counts
                    for (doc in snapshot.documents) {
                        val uids = doc.get("uids") as? List<*> ?: continue
                        val friendUid = uids.firstOrNull { it != currentUid }?.toString() ?: continue

                        try {
                            val friendProfile = firestore.collection("users").document(friendUid).get().await()
                            val friendUsername = friendProfile.getString("username") ?: "Friend"

                            val dailyDoc = firestore.collection("users")
                                .document(friendUid)
                                .collection("daily")
                                .document(todayDate)
                                .get()
                                .await()

                            val count = dailyDoc.getLong("count")?.toInt() ?: 0
                            val limit = dailyDoc.getLong("dailyLimit")?.toInt() ?: 50

                            friendsList.add(
                                FriendUser(
                                    uid = friendUid,
                                    username = friendUsername,
                                    displayName = friendUsername,
                                    todayCount = count,
                                    dailyLimit = limit,
                                    friendshipId = doc.id
                                )
                            )
                        } catch (e: Exception) {}
                    }

                    // Sort: LOWEST scroll count wins!
                    friendsList.sortBy { it.todayCount }
                    trySend(friendsList)
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getPendingRequests(): Flow<List<FriendUser>> = flow {
        emit(emptyList())
    }

    override fun acceptFriendRequest(friendshipId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Success(Unit))
    }
}
