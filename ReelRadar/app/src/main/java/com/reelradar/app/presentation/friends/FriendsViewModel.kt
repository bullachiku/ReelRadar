package com.reelradar.app.presentation.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelradar.app.core.util.Resource
import com.reelradar.app.domain.model.DailyRecord
import com.reelradar.app.domain.model.FriendUser
import com.reelradar.app.domain.model.User
import com.reelradar.app.domain.repository.FriendsRepository
import com.reelradar.app.domain.repository.ReelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class FriendsUiState(
    val leaderboard: List<FriendUser> = emptyList(),
    val todayRecord: DailyRecord = DailyRecord(date = ""),
    val streakInfo: com.reelradar.app.domain.model.StreakInfo = com.reelradar.app.domain.model.StreakInfo(),
    val searchResult: User? = null,
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val showAddDialog: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository,
    private val reelRepository: ReelRepository
) : ViewModel() {

    private val _searchState = MutableStateFlow<User?>(null)
    private val _isSearching = MutableStateFlow(false)
    private val _searchError = MutableStateFlow<String?>(null)
    private val _showAddDialog = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)

    private val todayDate = LocalDate.now().toString()

    val uiState: StateFlow<FriendsUiState> = combine(
        friendsRepository.getLeaderboard(todayDate),
        reelRepository.getTodayRecord(),
        reelRepository.getStreakInfo(),
        _searchState,
        _isSearching,
        _searchError,
        _showAddDialog
    ) { leaderboard, todayRecord, streak, searchResult, searching, error, dialog ->
        FriendsUiState(
            leaderboard = leaderboard,
            todayRecord = todayRecord,
            streakInfo = streak,
            searchResult = searchResult,
            isSearching = searching,
            searchError = error,
            showAddDialog = dialog,
            message = _message.value
        )
    }.stateIn(

        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FriendsUiState()
    )

    fun searchUsername(username: String) {
        if (username.isBlank()) return
        viewModelScope.launch {
            friendsRepository.searchUserByUsername(username.trim()).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _isSearching.value = true
                        _searchError.value = null
                    }
                    is Resource.Success -> {
                        _isSearching.value = false
                        _searchState.value = resource.data
                        if (resource.data == null) {
                            _searchError.value = "User not found"
                        }
                    }
                    is Resource.Error -> {
                        _isSearching.value = false
                        _searchError.value = resource.message
                    }
                }
            }
        }
    }

    fun addFriend(targetUser: User) {
        viewModelScope.launch {
            friendsRepository.sendFriendRequest(targetUser).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        _message.value = "Friend added successfully!"
                        _showAddDialog.value = false
                        _searchState.value = null
                    }
                    is Resource.Error -> {
                        _searchError.value = resource.message
                    }
                }
            }
        }
    }

    fun openAddDialog() {
        _showAddDialog.value = true
        _searchState.value = null
        _searchError.value = null
    }

    fun dismissAddDialog() {
        _showAddDialog.value = false
        _searchState.value = null
        _searchError.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    fun getShareSummaryText(): String {
        val record = uiState.value.todayRecord
        val mins = record.estimatedSeconds / 60
        return """
            🎯 ReelRadar Daily Stats
            📅 Date: ${record.date}
            ⚡ Reels Watched: ${record.count} / ${record.dailyLimit}
            ⏳ Mindful Time: ${mins}m
            🏆 Status: ${if (record.count <= record.dailyLimit) "Under limit! 🎉" else "Exceeded goal ⚠️"}
            
            Track and master your digital habits with ReelRadar!
        """.trimIndent()
    }
}
