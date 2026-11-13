package com.reelradar.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelradar.app.data.local.datastore.PreferencesManager
import com.reelradar.app.domain.model.User
import com.reelradar.app.domain.model.UserSettings
import com.reelradar.app.domain.repository.AuthRepository
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
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val settings: UserSettings = UserSettings(),
    val isExporting: Boolean = false,
    val csvExportData: String? = null,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val reelRepository: ReelRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    private val _csvData = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        authRepository.currentUserFlow,
        preferencesManager.userSettingsFlow,
        _message,
        _csvData
    ) { user, settings, msg, csv ->
        SettingsUiState(
            user = user,
            settings = settings,
            message = msg,
            csvExportData = csv
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun exportData() {
        viewModelScope.launch {
            val csv = reelRepository.exportDataToCsv()
            _csvData.value = csv
        }
    }

    fun clearCsvData() {
        _csvData.value = null
    }

    fun toggleDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            val current = uiState.value.settings
            preferencesManager.updateSettings(current.copy(darkTheme = isDark))
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onSignedOut()
        }
    }

    fun deleteAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            reelRepository.clearAllLocalData()
            authRepository.deleteAccount().collect {
                onComplete()
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
