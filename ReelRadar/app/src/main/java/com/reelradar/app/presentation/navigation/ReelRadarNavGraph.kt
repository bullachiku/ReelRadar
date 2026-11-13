package com.reelradar.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.reelradar.app.data.local.datastore.PreferencesManager
import com.reelradar.app.presentation.auth.AuthScreen
import com.reelradar.app.presentation.auth.AuthViewModel
import com.reelradar.app.presentation.friends.FriendsScreen
import com.reelradar.app.presentation.friends.FriendsViewModel
import com.reelradar.app.presentation.home.HomeScreen
import com.reelradar.app.presentation.home.HomeViewModel
import com.reelradar.app.presentation.insights.InsightsScreen
import com.reelradar.app.presentation.insights.InsightsViewModel
import com.reelradar.app.presentation.limits.LimitsScreen
import com.reelradar.app.presentation.limits.LimitsViewModel
import com.reelradar.app.presentation.onboarding.OnboardingScreen
import com.reelradar.app.presentation.settings.SettingsScreen
import com.reelradar.app.presentation.settings.SettingsViewModel
import com.reelradar.app.presentation.splash.SplashScreen

@Composable
fun ReelRadarNavGraph(
    navController: NavHostController = rememberNavController(),
    preferencesManager: PreferencesManager,
    auth: FirebaseAuth
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                preferencesManager = preferencesManager,
                auth = auth,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                preferencesManager = preferencesManager,
                onComplete = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Auth.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                onNavigateToLimits = { navController.navigate(Screen.Limits.route) },
                onNavigateToFriends = { navController.navigate(Screen.Friends.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Insights.route) {
            val insightsViewModel: InsightsViewModel = hiltViewModel()
            InsightsScreen(
                viewModel = insightsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Limits.route) {
            val limitsViewModel: LimitsViewModel = hiltViewModel()
            LimitsScreen(
                viewModel = limitsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Friends.route) {
            val friendsViewModel: FriendsViewModel = hiltViewModel()
            FriendsScreen(
                viewModel = friendsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
