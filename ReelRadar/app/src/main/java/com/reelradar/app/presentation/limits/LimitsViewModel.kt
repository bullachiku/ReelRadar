package com.reelradar.app.presentation.limits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelradar.app.data.local.datastore.PreferencesManager
import com.reelradar.app.domain.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

data class LimitsUiState(
    val settings: UserSettings = UserSettings(),
    val isUnlocked: Boolean = true,
    val showPinDialog: Boolean = false,
    val pinError: String? = null
)

@HiltViewModel
class LimitsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LimitsUiState())
    val uiState: StateFlow<LimitsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.userSettingsFlow.collect { settings ->
                _uiState.update { current ->
                    current.copy(
                        settings = settings,
                        isUnlocked = !settings.pinProtectionEnabled || current.isUnlocked
                    )
                }
            }
        }
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String): Boolean {
        val currentHash = _uiState.value.settings.pinHash
        val inputHash = hashPin(pin)
        return if (currentHash == inputHash) {
            _uiState.update { it.copy(isUnlocked = true, showPinDialog = false, pinError = null) }
            true
        } else {
            _uiState.update { it.copy(pinError = "Incorrect PIN") }
            false
        }
    }

    fun requestUnlock() {
        if (_uiState.value.settings.pinProtectionEnabled) {
            _uiState.update { it.copy(showPinDialog = true, pinError = null) }
        }
    }

    fun dismissPinDialog() {
        _uiState.update { it.copy(showPinDialog = false, pinError = null) }
    }

    fun updateDailyLimit(newLimit: Int) {
        if (!_uiState.value.isUnlocked) return
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(dailyLimit = newLimit)
            preferencesManager.updateSettings(updated)
        }
    }

    fun updateDwellTime(newDwellMs: Long) {
        if (!_uiState.value.isUnlocked) return
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(minDwellTimeMs = newDwellMs)
            preferencesManager.updateSettings(updated)
        }
    }

    fun toggleSoftWarning(enabled: Boolean) {
        if (!_uiState.value.isUnlocked) return
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(softWarningEnabled = enabled)
            preferencesManager.updateSettings(updated)
        }
    }

    fun toggleBreathingPause(enabled: Boolean) {
        if (!_uiState.value.isUnlocked) return
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(breathingPauseEnabled = enabled)
            preferencesManager.updateSettings(updated)
        }
    }

    fun toggleHardLock(enabled: Boolean) {
        if (!_uiState.value.isUnlocked) return
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(hardLockEnabled = enabled)
            preferencesManager.updateSettings(updated)
        }
    }

    fun setPinProtection(enabled: Boolean, pin: String = "") {
        viewModelScope.launch {
            val pinHash = if (enabled && pin.isNotBlank()) hashPin(pin) else ""
            val updated = _uiState.value.settings.copy(
                pinProtectionEnabled = enabled,
                pinHash = pinHash
            )
            preferencesManager.updateSettings(updated)
            _uiState.update { it.copy(isUnlocked = true) }
        }
    }
}
