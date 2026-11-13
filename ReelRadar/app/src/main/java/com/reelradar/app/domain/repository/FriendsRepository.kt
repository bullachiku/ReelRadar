package com.reelradar.app.domain.repository

import com.reelradar.app.core.util.Resource
import com.reelradar.app.domain.model.FriendUser
import com.reelradar.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface FriendsRepository {
    fun searchUserByUsername(username: String): Flow<Resource<User?>>
    fun sendFriendRequest(targetUser: User): Flow<Resource<Unit>>
    fun getLeaderboard(todayDate: String): Flow<List<FriendUser>>
    fun getPendingRequests(): Flow<List<FriendUser>>
    fun acceptFriendRequest(friendshipId: String): Flow<Resource<Unit>>
}
