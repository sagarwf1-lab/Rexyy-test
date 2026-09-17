package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.security.RootState
import com.example.features.assistant.AssistantViewModel
import com.example.features.notifications.RexyyNotificationListener
import com.example.ui.theme.RexyyBlue
import com.example.ui.theme.RexyyCardBorder
import com.example.ui.theme.RexyyCyan
import com.example.ui.theme.RexyyDarkBg
import com.example.ui.theme.RexyyEmerald
import com.example.ui.theme.RexyyError
import com.example.ui.theme.RexyySurface
import com.example.ui.theme.RexyySurfaceVariant
import com.example.ui.theme.RexyyTextMuted
import com.example.ui.theme.RexyyTextPrimary
import com.example.ui.theme.RexyyTextSecondary
import com.example.ui.theme.RexyyWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AssistantViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val aiModel by viewModel.userPrefs.aiModel.collectAsState()
    val wakeWordEnabled by viewModel.userPrefs.wakeWordEnabled.collectAsState()
    val speechLang by viewModel.userPrefs.speechLanguage.collectAsState()
    val ttsResponses by viewModel.userPrefs.ttsVoiceResponses.collectAsState()
    val ttsSpeed by viewModel.userPrefs.ttsSpeed.collectAsState()
    val ttsPitch by viewModel.userPrefs.ttsPitch.collectAsState()
    val rootState by viewModel.rootState.collectAsState()
    val rootAllowed by viewModel.userPrefs.rootModeAllowed.collectAsState()
    val historyEnabled by viewModel.userPrefs.historyEnabled.collectAsState()
    val notifReadingEnabled by viewModel.userPrefs.notificationReadingEnabled.collectAsState()

    val isTesting by viewModel.isTestingKey.collectAsState()
    val testResult by viewModel.testKeyResult.collectAsState()

    var showChangeKeyDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    var showLangMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RexyyDarkBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopAppBar(
                title = {
                    Text(
                        "Settings & Controls",
                        color = RexyyTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = RexyyCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RexyyDarkBg)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Device Capability Mode
                SettingsSectionCard(title = "Device Capability", icon = Icons.Default.Security) {
                    val isRooted = rootState == RootState.ROOT_PERMISSION_GRANTED
                    val isRootDetected = rootState == RootState.ROOT_DETECTED || rootState == RootState.ROOT_PERMISSION_DENIED

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RexyySurfaceVariant)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isRooted) RexyyEmerald else RexyyBlue)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isRooted) "ROOT MODE: Advanced System Mode" else "NON-ROOT: Standard Android Mode",
                                color = RexyyTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = when (rootState) {
                                    RootState.NOT_ROOTED -> "Standard secure Android environment"
                                    RootState.ROOT_DETECTED -> "Root access detected on device"
                                    RootState.ROOT_PERMISSION_DENIED -> "Root permission currently denied"
                                    RootState.ROOT_PERMISSION_GRANTED -> "Full root-level command layer active"
                                    RootState.UNKNOWN -> "Checking environment capabilities..."
                                },
                                color = RexyyTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (isRootDetected && !isRooted) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.requestRootPermission() },
                            colors = ButtonDefaults.buttonColors(containerColor = RexyyWarning),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_root_button")
                        ) {
                            Text("Grant Root Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Section 2: AI Brain Configuration
                SettingsSectionCard(title = "AI Brain", icon = Icons.Default.Psychology) {
                    // Provider Info
                    SettingsRow(label = "Provider", value = "OpenAI")

                    // Model Selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showModelMenu = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Model", color = RexyyTextPrimary, fontSize = 14.sp)
                        Box {
                            Text(aiModel, color = RexyyCyan, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            DropdownMenu(
                                expanded = showModelMenu,
                                onDismissRequest = { showModelMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("gpt-4o-mini (Fast & Recommended)") },
                                    onClick = {
                                        viewModel.userPrefs.setAiModel("gpt-4o-mini")
                                        showModelMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("gpt-4o (High Reasoning)") },
                                    onClick = {
                                        viewModel.userPrefs.setAiModel("gpt-4o")
                                        showModelMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // API Key Status
                    SettingsRow(
                        label = "API Key Vault",
                        value = viewModel.keyStoreManager.getMaskedApiKey()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showChangeKeyDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("change_api_key_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Key", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val currentKey = viewModel.keyStoreManager.getApiKey()
                                if (!currentKey.isNullOrBlank()) {
                                    viewModel.testApiKey(currentKey)
                                }
                            },
                            enabled = !isTesting && viewModel.keyStoreManager.isApiKeyConfigured(),
                            colors = ButtonDefaults.buttonColors(containerColor = RexyyCyan, contentColor = Color(0xFF021B17)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_saved_key_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                            } else {
                                Text("Test Connection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Feedback banner
                    AnimatedVisibility(visible = testResult != null) {
                        testResult?.let { (success, msg) ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = msg,
                                color = if (success) RexyyEmerald else RexyyError,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Section 3: Voice & Speech Engine
                SettingsSectionCard(title = "Voice & Speech Engine", icon = Icons.Default.RecordVoiceOver) {
                    // Wake Word Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Wake Word Detection", color = RexyyTextPrimary, fontSize = 14.sp)
                            Text("Trigger on 'Hello REXYY'", color = RexyyTextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = wakeWordEnabled,
                            onCheckedChange = { viewModel.userPrefs.setWakeWordEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = RexyyCyan, checkedTrackColor = RexyyCyan.copy(alpha = 0.4f))
                        )
                    }

                    // Speech Language Picker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLangMenu = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Speech Recognition Language", color = RexyyTextPrimary, fontSize = 14.sp)
                            Text("Hindi, Hinglish, English, Bengali, Urdu", color = RexyyTextSecondary, fontSize = 11.sp)
                        }
                        Box {
                            Text(speechLang, color = RexyyCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            DropdownMenu(
                                expanded = showLangMenu,
                                onDismissRequest = { showLangMenu = false }
                            ) {
                                listOf(
                                    "en-IN" to "English (India) / Hinglish",
                                    "hi-IN" to "Hindi (India)",
                                    "en-US" to "English (US)",
                                    "bn-IN" to "Bengali (India)",
                                    "ur-PK" to "Urdu"
                                ).forEach { (code, label) ->
                                    DropdownMenuItem(
                                        text = { Text("$label ($code)") },
                                        onClick = {
                                            viewModel.userPrefs.setSpeechLanguage(code)
                                            showLangMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Spoken Responses Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Voice Audio Responses", color = RexyyTextPrimary, fontSize = 14.sp)
                        Switch(
                            checked = ttsResponses,
                            onCheckedChange = { viewModel.userPrefs.setTtsVoiceResponses(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = RexyyCyan, checkedTrackColor = RexyyCyan.copy(alpha = 0.4f))
                        )
                    }

                    // Speech Speed Slider
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Speech Speed", color = RexyyTextPrimary, fontSize = 13.sp)
                            Text("${"%.2f".format(ttsSpeed)}x", color = RexyyCyan, fontSize = 13.sp)
                        }
                        Slider(
                            value = ttsSpeed,
                            onValueChange = { viewModel.userPrefs.setTtsSpeed(it) },
                            valueRange = 0.75f..1.5f,
                            colors = SliderDefaults.colors(thumbColor = RexyyCyan, activeTrackColor = RexyyCyan)
                        )
                    }
                }

                // Section 4: System Permissions
                SettingsSectionCard(title = "System Permissions", icon = Icons.Default.Build) {
                    PermissionActionRow(
                        name = "Microphone Access",
                        status = "Required for voice interaction",
                        onClick = {
                            openAppSettings(context)
                        }
                    )
                    PermissionActionRow(
                        name = "Notification Listener",
                        status = if (RexyyNotificationListener.isPermissionGranted(context)) "Granted" else "Requires manual setup",
                        onClick = {
                            RexyyNotificationListener.openNotificationAccessSettings(context)
                        }
                    )
                    PermissionActionRow(
                        name = "Exact Alarm & Reminders",
                        status = "System scheduling permission",
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } else {
                                openAppSettings(context)
                            }
                        }
                    )
                }

                // Section 5: Privacy & Local Data
                SettingsSectionCard(title = "Privacy & Local Vault", icon = Icons.Default.Security) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Record Conversation History", color = RexyyTextPrimary, fontSize = 14.sp)
                        Switch(
                            checked = historyEnabled,
                            onCheckedChange = { viewModel.userPrefs.setHistoryEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = RexyyCyan, checkedTrackColor = RexyyCyan.copy(alpha = 0.4f))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.clearHistory() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RexyyWarning),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_history_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Local Conversation History", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showResetConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RexyyError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_app_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset API Credentials & First Launch", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Change Key Dialog
    if (showChangeKeyDialog) {
        var newKey by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangeKeyDialog = false },
            title = { Text("Change OpenAI API Key", color = RexyyTextPrimary) },
            text = {
                Column {
                    Text("Enter new API key to be encrypted in Android Keystore:", color = RexyyTextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it.trim() },
                        placeholder = { Text("sk-...", color = RexyyTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = RexyyTextPrimary,
                            unfocusedTextColor = RexyyTextPrimary,
                            focusedBorderColor = RexyyCyan,
                            unfocusedBorderColor = RexyyCardBorder
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKey.isNotBlank()) {
                            viewModel.keyStoreManager.saveApiKey(newKey)
                            showChangeKeyDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RexyyCyan, contentColor = Color(0xFF021B17))
                ) {
                    Text("Save Securely")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeKeyDialog = false }) {
                    Text("Cancel", color = RexyyTextSecondary)
                }
            },
            containerColor = RexyySurface
        )
    }

    // Reset Confirm Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset Setup?", color = RexyyTextPrimary) },
            text = {
                Text(
                    "This will clear the encrypted API key from Keystore and reset REXYY to the first-launch setup screen.",
                    color = RexyyTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.keyStoreManager.clearApiKey()
                        viewModel.userPrefs.clearAllPreferences()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RexyyError)
                ) {
                    Text("Reset All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = RexyyTextSecondary)
                }
            },
            containerColor = RexyySurface
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RexyySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RexyyCardBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = RexyyCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, color = RexyyCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = RexyyTextPrimary, fontSize = 14.sp)
        Text(value, color = RexyyTextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun PermissionActionRow(name: String, status: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = RexyyTextPrimary, fontSize = 14.sp)
            Text(status, color = RexyyTextSecondary, fontSize = 11.sp)
        }
        Text("Configure", color = RexyyCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
