package com.reelradar.app.presentation.limits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitsScreen(
    viewModel: LimitsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var pinInput by remember { mutableStateOf("") }
    var newPinSetup by remember { mutableStateOf("") }
    var showSetupPinDialog by remember { mutableStateOf(false) }

    // PIN Unlock Dialog
    if (uiState.showPinDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPinDialog() },
            title = { Text("Tamper Protection") },
            text = {
                Column {
                    Text("Enter your 4-digit PIN to modify limits.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        label = { Text("PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = uiState.pinError != null,
                        singleLine = true
                    )
                    if (uiState.pinError != null) {
                        Text(uiState.pinError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (viewModel.verifyPin(pinInput)) {
                        pinInput = ""
                    }
                }) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPinDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Set Up New PIN Dialog
    if (showSetupPinDialog) {
        AlertDialog(
            onDismissRequest = { showSetupPinDialog = false },
            title = { Text("Set 4-Digit Protection PIN") },
            text = {
                Column {
                    Text("Choose a 4-digit PIN to lock your limits against impulsive changes.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPinSetup,
                        onValueChange = { if (it.length <= 4) newPinSetup = it },
                        label = { Text("4-digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinSetup.length == 4) {
                            viewModel.setPinProtection(true, newPinSetup)
                            showSetupPinDialog = false
                            newPinSetup = ""
                        }
                    },
                    enabled = newPinSetup.length == 4
                ) {
                    Text("Enable PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetupPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Limits & Friction Rules", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (uiState.settings.pinProtectionEnabled) {
                        IconButton(onClick = { viewModel.requestUnlock() }) {
                            Icon(
                                imageVector = if (uiState.isUnlocked) Icons.Default.Shield else Icons.Default.Lock,
                                contentDescription = "PIN Lock",
                                tint = if (uiState.isUnlocked) Color(0xFF00B894) else Color(0xFFFF7675)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tamper Lock Status Indicator
            if (uiState.settings.pinProtectionEnabled && !uiState.isUnlocked) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2135)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFD79A8))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Limits are Protected by PIN", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("Tap the lock icon above or Unlock to modify.", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { viewModel.requestUnlock() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Unlock", fontSize = 11.sp)
                        }
                    }
                }
            }

            // 1. Daily Limit Slider
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Reel Limit", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text("Target maximum reels per day", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(
                            text = "${uiState.settings.dailyLimit} Reels",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6C5CE7)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Slider(
                        value = uiState.settings.dailyLimit.toFloat(),
                        onValueChange = { viewModel.updateDailyLimit(it.toInt()) },
                        valueRange = 10f..200f,
                        steps = 18,
                        enabled = uiState.isUnlocked,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF6C5CE7),
                            activeTrackColor = Color(0xFF6C5CE7)
                        )
                    )
                }
            }

            // 2. Minimum Dwell Time Filter
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Minimum Dwell Time", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text("Ignore swiped reels watched under this threshold", fontSize = 12.sp, color = Color.Gray)
                        }
                        val seconds = uiState.settings.minDwellTimeMs / 1000f
                        Text(
                            text = "%.1fs".format(seconds),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00CEC9)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Slider(
                        value = (uiState.settings.minDwellTimeMs / 1000f),
                        onValueChange = { viewModel.updateDwellTime((it * 1000).toLong()) },
                        valueRange = 1.0f..5.0f,
                        steps = 7,
                        enabled = uiState.isUnlocked,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00CEC9),
                            activeTrackColor = Color(0xFF00CEC9)
                        )
                    )
                }
            }

            // 3. Escalating Friction Ladder
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Escalating Friction Pipeline", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text("Friction progressively builds to naturally break trance-scrolling", fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step 1: Soft warning at 70%
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFFDCB6E), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("70% Soft Warning", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Gentle heads-up notification", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = uiState.settings.softWarningEnabled,
                            onCheckedChange = { viewModel.toggleSoftWarning(it) },
                            enabled = uiState.isUnlocked,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFDCB6E))
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 2: 10s Breathing Pause
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Air, contentDescription = null, tint = Color(0xFF00CEC9), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("10s Breathing Pause at 100%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Mindful screen overlay to take a deep breath", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = uiState.settings.breathingPauseEnabled,
                            onCheckedChange = { viewModel.toggleBreathingPause(it) },
                            enabled = uiState.isUnlocked,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00CEC9))
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 3: Hard Lock Screen Overlay
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFF7675), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Hard Lock Overlay", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Blocks reels screen (Max 2 snoozes of 5 min/day)", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = uiState.settings.hardLockEnabled,
                            onCheckedChange = { viewModel.toggleHardLock(it) },
                            enabled = uiState.isUnlocked,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF7675))
                        )
                    }
                }
            }

            // 4. Tamper Resistance PIN
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF6C5CE7), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Tamper Resistance PIN", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Require PIN to change limits or disable friction", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = uiState.settings.pinProtectionEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    showSetupPinDialog = true
                                } else {
                                    if (uiState.isUnlocked) {
                                        viewModel.setPinProtection(false)
                                    } else {
                                        viewModel.requestUnlock()
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6C5CE7))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
