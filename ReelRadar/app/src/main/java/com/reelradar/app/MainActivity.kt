package com.reelradar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.reelradar.app.core.theme.ReelRadarTheme
import com.reelradar.app.data.local.datastore.PreferencesManager
import com.reelradar.app.presentation.navigation.ReelRadarNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by preferencesManager.userSettingsFlow.collectAsState(initial = null)
            val isDarkTheme = settings?.darkTheme ?: true

            ReelRadarTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReelRadarNavGraph(
                        preferencesManager = preferencesManager,
                        auth = auth
                    )
                }
            }
        }
    }
}
