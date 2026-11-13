package com.reelradar.app.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Onboarding : Screen("onboarding_screen")
    object Auth : Screen("auth_screen")
    object Home : Screen("home_screen")
    object Insights : Screen("insights_screen")
    object Limits : Screen("limits_screen")
    object Friends : Screen("friends_screen")
    object Settings : Screen("settings_screen")
}
