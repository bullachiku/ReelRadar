package com.reelradar.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.reelradar.app.core.util.Constants
import com.reelradar.app.domain.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFERENCES_NAME)

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        // Settings Keys
        val KEY_DAILY_LIMIT = intPreferencesKey("daily_limit")
        val KEY_MIN_DWELL_MS = longPreferencesKey("min_dwell_ms")
        val KEY_SOFT_WARNING = booleanPreferencesKey("soft_warning_enabled")
        val KEY_BREATHING_PAUSE = booleanPreferencesKey("breathing_pause_enabled")
        val KEY_HARD_LOCK = booleanPreferencesKey("hard_lock_enabled")
        val KEY_MINDFUL_GATE = booleanPreferencesKey("mindful_gate_enabled")
        val KEY_OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val KEY_PIN_PROTECTION = booleanPreferencesKey("pin_protection_enabled")
        val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        val KEY_MAX_SNOOZES = intPreferencesKey("max_snoozes_per_day")
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")

        // Snooze tracking
        val KEY_SNOOZE_COUNT_TODAY = intPreferencesKey("snooze_count_today")
        val KEY_LAST_SNOOZE_DATE = stringPreferencesKey("last_snooze_date")
        val KEY_OVERLAY_POSITION_X = intPreferencesKey("overlay_position_x")
        val KEY_OVERLAY_POSITION_Y = intPreferencesKey("overlay_position_y")
    }

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val userSettingsFlow: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            dailyLimit = prefs[KEY_DAILY_LIMIT] ?: Constants.DEFAULT_DAILY_LIMIT,
            minDwellTimeMs = prefs[KEY_MIN_DWELL_MS] ?: Constants.DEFAULT_MIN_DWELL_TIME_MS,
            softWarningEnabled = prefs[KEY_SOFT_WARNING] ?: true,
            breathingPauseEnabled = prefs[KEY_BREATHING_PAUSE] ?: true,
            hardLockEnabled = prefs[KEY_HARD_LOCK] ?: false,
            mindfulGateEnabled = prefs[KEY_MINDFUL_GATE] ?: false,
            overlayEnabled = prefs[KEY_OVERLAY_ENABLED] ?: true,
            pinProtectionEnabled = prefs[KEY_PIN_PROTECTION] ?: false,
            pinHash = prefs[KEY_PIN_HASH] ?: "",
            maxSnoozesPerDay = prefs[KEY_MAX_SNOOZES] ?: 2,
            darkTheme = prefs[KEY_DARK_THEME] ?: true
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun updateSettings(settings: UserSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_DAILY_LIMIT] = settings.dailyLimit
            prefs[KEY_MIN_DWELL_MS] = settings.minDwellTimeMs
            prefs[KEY_SOFT_WARNING] = settings.softWarningEnabled
            prefs[KEY_BREATHING_PAUSE] = settings.breathingPauseEnabled
            prefs[KEY_HARD_LOCK] = settings.hardLockEnabled
            prefs[KEY_MINDFUL_GATE] = settings.mindfulGateEnabled
            prefs[KEY_OVERLAY_ENABLED] = settings.overlayEnabled
            prefs[KEY_PIN_PROTECTION] = settings.pinProtectionEnabled
            prefs[KEY_PIN_HASH] = settings.pinHash
            prefs[KEY_MAX_SNOOZES] = settings.maxSnoozesPerDay
            prefs[KEY_DARK_THEME] = settings.darkTheme
        }
    }

    suspend fun saveOverlayPosition(x: Int, y: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_OVERLAY_POSITION_X] = x
            prefs[KEY_OVERLAY_POSITION_Y] = y
        }
    }

    val overlayPosition: Flow<Pair<Int, Int>> = dataStore.data.map { prefs ->
        val x = prefs[KEY_OVERLAY_POSITION_X] ?: 100
        val y = prefs[KEY_OVERLAY_POSITION_Y] ?: 300
        Pair(x, y)
    }

    suspend fun getTodaySnoozeCount(): Int {
        val today = LocalDate.now().toString()
        var count = 0
        dataStore.edit { prefs ->
            val lastDate = prefs[KEY_LAST_SNOOZE_DATE]
            if (lastDate != today) {
                prefs[KEY_LAST_SNOOZE_DATE] = today
                prefs[KEY_SNOOZE_COUNT_TODAY] = 0
                count = 0
            } else {
                count = prefs[KEY_SNOOZE_COUNT_TODAY] ?: 0
            }
        }
        return count
    }

    suspend fun incrementSnoozeCount(): Boolean {
        val today = LocalDate.now().toString()
        var canSnooze = false
        dataStore.edit { prefs ->
            val lastDate = prefs[KEY_LAST_SNOOZE_DATE]
            val maxSnoozes = prefs[KEY_MAX_SNOOZES] ?: 2
            val currentCount = if (lastDate == today) prefs[KEY_SNOOZE_COUNT_TODAY] ?: 0 else 0
            if (currentCount < maxSnoozes) {
                prefs[KEY_LAST_SNOOZE_DATE] = today
                prefs[KEY_SNOOZE_COUNT_TODAY] = currentCount + 1
                canSnooze = true
            } else {
                canSnooze = false
            }
        }
        return canSnooze
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
