package com.reelradar.app.presentation.home

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelradar.app.core.util.Constants
import com.reelradar.app.data.local.datastore.PreferencesManager
import com.reelradar.app.domain.model.DailyRecord
import com.reelradar.app.domain.model.StreakInfo
import com.reelradar.app.domain.model.UserSettings
import com.reelradar.app.domain.repository.ReelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val todayRecord: DailyRecord = DailyRecord(date = ""),
    val streakInfo: StreakInfo = StreakInfo(),
    val settings: UserSettings = UserSettings(),
    val isAccessibilityEnabled: Boolean = false,
    val timeLostFormatted: String = "0m",
    val badges: List<com.reelradar.app.domain.model.Badge> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val reelRepository: ReelRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled = _isAccessibilityEnabled.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        reelRepository.getTodayRecord(),
        reelRepository.getStreakInfo(),
        preferencesManager.userSettingsFlow,
        reelRepository.getAllRecords(),
        _isAccessibilityEnabled
    ) { record, streak, settings, allRecords, isServiceActive ->
        val totalSeconds = record.estimatedSeconds.takeIf { it > 0 }
            ?: (record.count * Constants.DEFAULT_AVG_DWELL_SECONDS.toLong())
        val hours = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val timeFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        val badges = com.reelradar.app.domain.model.BadgesList.calculateBadges(streak, allRecords)

        HomeUiState(
            todayRecord = record,
            streakInfo = streak,
            settings = settings,
            isAccessibilityEnabled = isServiceActive,
            timeLostFormatted = timeFormatted,
            badges = badges
        )
    }.stateIn(

        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun checkAccessibilityPermission(context: Context) {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val isEnabled = enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
        _isAccessibilityEnabled.value = isEnabled
    }

    fun toggleOverlay(enabled: Boolean) {
        viewModelScope.launch {
            val current = uiState.value.settings
            preferencesManager.updateSettings(current.copy(overlayEnabled = enabled))
        }
    }

    fun toggleMindfulGate(enabled: Boolean) {
        viewModelScope.launch {
            val current = uiState.value.settings
            preferencesManager.updateSettings(current.copy(mindfulGateEnabled = enabled))
        }
    }
}
