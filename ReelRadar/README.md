# ReelRadar 🎯

A privacy-first Instagram Reels scroll counter with smart limits, escalating friction, digital wellness insights, and offline-first cloud sync for Android.

Built with **Kotlin**, **Jetpack Compose (Material 3)**, **MVVM + Clean Architecture**, **Hilt DI**, **Room Database**, **DataStore Preferences**, **WorkManager**, and **Firebase (Auth, Cloud Firestore, Remote Config, Crashlytics, Cloud Messaging)**.

---

## Architecture & Package Structure

```
com.reelradar.app/
├── core/
│   ├── detector/
│   │   ├── DetectionResult.kt       # Scroll detection outcome state
│   │   └── ReelDetector.kt          # Dwell-time & deduplication counting engine
│   ├── di/
│   │   ├── AppModule.kt             # Application context & Dispatchers
│   │   ├── DatabaseModule.kt        # Room DB & DAO injection
│   │   ├── FirebaseModule.kt        # Firebase services injection
│   │   └── RepositoryModule.kt      # Hilt Repository bindings
│   ├── theme/
│   │   ├── Color.kt                 # Cyber Zen palette
│   │   ├── Shape.kt
│   │   ├── Theme.kt                 # Dynamic light/dark Material 3 theme
│   │   └── Type.kt
│   └── util/
│       ├── Constants.kt             # App keys & defaults
│       └── Resource.kt              # State wrapper (Success, Error, Loading)
├── data/
│   ├── local/
│   │   ├── datastore/
│   │   │   └── PreferencesManager.kt # DataStore settings & positions
│   │   └── db/
│   │       ├── converter/
│   │       │   └── Converters.kt     # Room type converters (Hourly lists)
│   │       ├── dao/
│   │       │   ├── DailyRecordDao.kt # Local daily stats queries
│   │       │   └── SessionDao.kt     # Session duration & streaks
│   │       ├── entity/
│   │       │   ├── DailyRecordEntity.kt
│   │       │   └── ReelSessionEntity.kt
│   │       └── ReelRadarDatabase.kt
│   ├── remote/
│   │   └── dto/
│   │       ├── FriendshipDto.kt
│   │       └── UserDto.kt           # Firestore models
│   ├── repository/
│   │   ├── AuthRepositoryImpl.kt    # Firebase Auth + Firestore profiles
│   │   ├── FriendsRepositoryImpl.kt # Mutual leaderboard queries
│   │   └── ReelRepositoryImpl.kt    # Offline-first Room source of truth
│   └── sync/
│       ├── ReelSyncWorker.kt        # Delta cloud sync with conflict resolution
│       └── SyncManager.kt           # Periodic & immediate WorkManager triggers
├── domain/
│   ├── model/
│   │   ├── DailyRecord.kt
│   │   ├── FriendUser.kt
│   │   ├── ReelSession.kt
│   │   ├── StreakInfo.kt
│   │   └── User.kt
│   └── repository/
│       ├── AuthRepository.kt
│       ├── FriendsRepository.kt
│       └── ReelRepository.kt
├── presentation/
│   ├── auth/
│   │   ├── AuthScreen.kt
│   │   ├── AuthState.kt
│   │   └── AuthViewModel.kt
│   ├── friends/
│   │   ├── FriendsScreen.kt
│   │   └── FriendsViewModel.kt
│   ├── home/
│   │   ├── HomeScreen.kt            # Progress ring, status banner, time lost
│   │   └── HomeViewModel.kt
│   ├── insights/
│   │   ├── InsightsScreen.kt        # 24h distribution, weekday heatmap, peak doomscroll
│   │   └── InsightsViewModel.kt
│   ├── limits/
│   │   ├── LimitsScreen.kt          # Escalating friction ladder & PIN lock
│   │   └── LimitsViewModel.kt
│   ├── navigation/
│   │   ├── ReelRadarNavGraph.kt     # Compose navigation controller
│   │   └── Screen.kt
│   ├── onboarding/
│   │   └── OnboardingScreen.kt      # Honest permission rationale & deep links
│   ├── settings/
│   │   ├── SettingsScreen.kt        # Data export (CSV) & wipe
│   │   └── SettingsViewModel.kt
│   └── widget/
│       ├── ReelRadarWidget.kt       # Glance Material 3 AppWidget
│       └── ReelRadarWidgetReceiver.kt
└── service/
    ├── ReelAccessibilityService.kt  # Non-intrusive Instagram gesture sensor
    └── ReelOverlayService.kt        # Floating HUD, 10s breathing pause, hard lock
```

---

## Features

1. **Pure Structural Reel Detection**:
   - Strictly monitors `com.instagram.android`.
   - Never reads or inspects usernames, captions, video content, DMs, or audio.
   - Configurable minimum dwell time (default `1.5s`) to filter out quick skips.
   - De-duplicates rapid back-and-forth swipes (threshold `1.2s`).
   - Dynamic detection rules via Firebase Remote Config with safe local fallbacks.
2. **Escalating Friction Pipeline**:
   - `70%`: Soft warning notification.
   - `100%`: Daily limit alert.
   - `Breathing Pause`: Mandatory 10-second reflection countdown overlay.
   - `Hard Lock Overlay`: Optional screen blocker with maximum 2 snoozes of 5 minutes per day.
3. **Mindful Intention Gate**:
   - Prompts the user before scrolling ("Set an intention: 10, 25, or 50 reels").
4. **Draggable Floating HUD**:
   - Real-time overlay pill (`X / Y`) on top of Instagram, repositionable via drag gestures.
5. **Insights & Analytics**:
   - 24-hour activity distribution bar chart.
   - Monday–Sunday weekday scroll intensity heatmap.
   - Peak doomscroll window calculation (e.g. `10:00 PM – 12:00 AM`).
   - Longest session sitting duration.
   - Weekly recap summary (% change vs previous week).
6. **Friends Leaderboard**:
   - Lowest scroll count ranks #1.
   - One-click share-card generator for social media.
7. **Glance Home Screen Widget**:
   - Interactive progress ring and counter directly on the Android home screen.
8. **Tamper Resistance**:
   - Optional 4-digit PIN lock required to change limits or disable friction.
9. **Data Export & Privacy**:
   - Export full history to CSV.
   - One-click complete account and data wipe.

---

## Build & Run Instructions

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or newer.
- Android SDK 35 (`compileSdk 35`, `targetSdk 35`, `minSdk 26`).
- JDK 17.

### Running in Android Studio
1. Open Android Studio and select **Open**.
2. Navigate to `C:\Users\mt089\.gemini\antigravity\scratch\ReelRadar`.
3. Let Gradle sync dependencies.
4. Replace `app/google-services.json` with your real Firebase config from the Firebase Console.
5. Connect an Android device or start an emulator running API 26+.
6. Click **Run > Run 'app'** (`Shift + F10`).

---

## Testing on a Real Device

1. **Launch App**: Open ReelRadar and proceed through Onboarding.
2. **Grant Permissions**:
   - Tap **Enable Accessibility Detector**: In Android Settings > Accessibility > Downloaded Apps > Select **ReelRadar Scroll Detector** > Enable.
   - Tap **Enable Floating Overlay**: Allow "Display over other apps" in Settings.
   - On Android 13+, grant Notification permissions when prompted.
3. **Test Gesture Detection**:
   - Open **Instagram** and go to the **Reels** tab.
   - Notice the floating counter badge showing `0 / 50`.
   - Scroll to the next reel and stay for more than 1.5 seconds.
   - The counter updates to `1 / 50`.
   - Rapidly swipe back and forth — notice the count does not falsely increment.
4. **Test Friction Ladder**:
   - Lower the daily limit in **Limits & Rules** to `3 reels`.
   - Scroll 3 reels in Instagram.
   - The 10-second breathing pause overlay will smoothly appear over Instagram!

---

## Troubleshooting Guide

| Issue | Cause | Solution |
| :--- | :--- | :--- |
| **Service disabled after app update or device restart** | Android battery optimization / OEM task killers killing background services | Disable battery optimization for ReelRadar: Go to Settings > Apps > ReelRadar > Battery > Set to **Unrestricted**. |
| **Floating HUD does not appear** | Overlay permission not granted | Go to Settings > Apps > Special app access > **Display over other apps** > Enable ReelRadar. |
| **Reels count not updating** | Instagram changed view IDs or user swiping faster than dwell threshold | Check Remote Config in Firebase Console to update `reels_pager_view_ids`. Also verify you stay on each reel for >= 1.5s. |
| **WorkManager cloud sync delayed** | Android Doze mode or network restrictions | Cloud sync is designed to be batched and offline-first; it runs every 4 hours when network is connected. Use "Sync Immediately" or open the app to trigger a sync. |

---

## Play Store Declaration & Compliance

### 1. Accessibility Service Declaration Text (For Google Play Console)

> **Prominent Disclosure**:
> ReelRadar uses the Android AccessibilityService API solely to detect when the user is actively viewing and scrolling through Instagram Reels (`com.instagram.android`).
> 
> **What ReelRadar tracks**:
> - Structural scroll and page-change events within Instagram Reels to increment your personal scroll counter.
> - Dwell time per video to estimate mindful screen time.
> 
> **What ReelRadar DOES NOT do**:
> - ReelRadar **DOES NOT** read, intercept, or store any text, captions, comments, usernames, private messages, search queries, or video media.
> - ReelRadar does not monitor any other application on your device.
> - ReelRadar does not perform automated clicks, gestures, or modifications to Instagram.
> 
> All scroll detection is computed locally on your device to support self-imposed digital wellness goals and screen time limits.

### 2. Privacy Policy Draft

> **ReelRadar Privacy Policy**  
> *Last Updated: November 2026*  
> 
> At ReelRadar, we believe digital wellbeing tools must respect personal boundaries:
> 1. **No Content Interception**: We never read or transmit what you watch, who you follow, captions, or private messages. ReelRadar only detects the structural gesture of swiping between video containers in Instagram.
> 2. **Local-First Processing**: Your scroll counts and time logs are stored locally on your device in a secure SQLite Room database.
> 3. **Optional Cloud Backup**: If you create an account, only numeric daily counts and your chosen username are synchronized with your private Google Cloud Firestore collection.
> 4. **No Third-Party Tracking**: We do not sell data to advertisers, data brokers, or marketing networks.
> 5. **Complete Data Deletion**: You can export your full history to CSV or permanently delete all your data and account at any time with one tap from Settings.
