package com.example.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.security.RootState
import com.example.core.voice.AssistantState
import com.example.features.assistant.AssistantViewModel
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

@Composable
fun HomeScreen(
    viewModel: AssistantViewModel,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenReminders: () -> Unit
) {
    val context = LocalContext.current
    val assistantState by viewModel.assistantState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val lastCommand by viewModel.lastCommand.collectAsState()
    val lastResponse by viewModel.lastResponse.collectAsState()
    val rmsDb by viewModel.rmsDb.collectAsState()
    val rootState by viewModel.rootState.collectAsState()
    val isServiceActive by viewModel.userPrefs.serviceActive.collectAsState()
    val confirmationState by viewModel.confirmationState.collectAsState()

    var manualTextInput by remember { mutableStateOf("") }

    // Permission launcher for Record Audio & Post Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordAudioGranted) {
            viewModel.startListeningForWakeWordOrCommand()
        }
    }

    val checkAndStartListening = {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            if (assistantState == AssistantState.LISTENING) {
                viewModel.stopListening()
            } else {
                viewModel.startListeningForWakeWordOrCommand()
            }
        } else {
            val neededPermissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(neededPermissions.toTypedArray())
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RexyyDarkBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand Name & Status Pill
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "REXYY",
                            color = RexyyCyan,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Root Mode Badge
                        val isRootGranted = rootState == RootState.ROOT_PERMISSION_GRANTED
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isRootGranted) RexyyEmerald.copy(alpha = 0.15f)
                                    else RexyyBlue.copy(alpha = 0.15f)
                                )
                                .border(
                                    1.dp,
                                    if (isRootGranted) RexyyEmerald.copy(alpha = 0.5f)
                                    else RexyyBlue.copy(alpha = 0.4f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isRootGranted) "ROOT" else "NON-ROOT",
                                color = if (isRootGranted) RexyyEmerald else RexyyBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // User greeting subtitle
                    Text(
                        text = "Hello Sagar Sir",
                        color = RexyyTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Action Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Background Service Toggle Badge
                    IconButton(
                        onClick = { viewModel.toggleVoiceService(context) },
                        modifier = Modifier.testTag("toggle_voice_service_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Voice Service",
                            tint = if (isServiceActive) RexyyCyan else RexyyTextMuted
                        )
                    }

                    IconButton(
                        onClick = onOpenReminders,
                        modifier = Modifier.testTag("home_reminders_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Reminders",
                            tint = RexyyTextSecondary
                        )
                    }

                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.testTag("home_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = RexyyTextSecondary
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = RexyyTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CENTER AI CORE & WAVEFORM
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // Animated AI Core
                AnimatedAiCore(
                    state = assistantState,
                    rmsDb = rmsDb,
                    onClick = { checkAndStartListening() }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Voice Waveform Bars
                VoiceWaveform(
                    state = assistantState,
                    rmsDb = rmsDb
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Live Status Message
                Text(
                    text = statusMessage,
                    color = when (assistantState) {
                        AssistantState.LISTENING -> RexyyCyan
                        AssistantState.THINKING -> RexyyBlue
                        AssistantState.SPEAKING -> RexyyEmerald
                        AssistantState.ERROR -> RexyyError
                        AssistantState.IDLE -> RexyyTextSecondary
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Mandatory Identity Tag
                Text(
                    text = "Created by Sagar",
                    color = RexyyTextMuted,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LOWER SECTION: Suggestion Chips, Last Card, and Input Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Quick Suggestion Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "YouTube kholo",
                        "Volume 50 percent karo",
                        "Google par search karo",
                        "Kal 8 baje yaad dilana",
                        "Notifications padho",
                        "Camera open karo"
                    ).forEach { chipText ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(RexyySurfaceVariant)
                                .border(1.dp, RexyyCardBorder, RoundedCornerShape(20.dp))
                                .clickable { viewModel.executeCommand(chipText) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = chipText,
                                color = RexyyTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Last Command & Response Card
                if (!lastCommand.isNullOrBlank() || !lastResponse.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = RexyySurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RexyyCardBorder))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (!lastCommand.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Sagar Sir:", color = RexyyBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(lastCommand!!, color = RexyyTextPrimary, fontSize = 13.sp)
                                }
                            }

                            if (!lastResponse.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("REXYY:", color = RexyyCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(lastResponse!!, color = RexyyTextPrimary, fontSize = 13.sp, lineHeight = 18.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Bottom Manual Text Input Bar & Mic Trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualTextInput,
                        onValueChange = { manualTextInput = it },
                        placeholder = {
                            Text(
                                "Type command or question...",
                                color = RexyyTextMuted,
                                fontSize = 13.sp
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (manualTextInput.isNotBlank()) {
                                    viewModel.executeCommand(manualTextInput)
                                    manualTextInput = ""
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = RexyyTextPrimary,
                            unfocusedTextColor = RexyyTextPrimary,
                            focusedBorderColor = RexyyCyan,
                            unfocusedBorderColor = RexyyCardBorder,
                            focusedContainerColor = RexyySurface,
                            unfocusedContainerColor = RexyySurface
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_command_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (manualTextInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.executeCommand(manualTextInput)
                                manualTextInput = ""
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(RexyyCyan)
                                .testTag("send_command_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF021B17))
                        }
                    } else {
                        IconButton(
                            onClick = { checkAndStartListening() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (assistantState == AssistantState.LISTENING) RexyyError else RexyyCyan)
                                .testTag("toggle_mic_button")
                        ) {
                            Icon(
                                imageVector = if (assistantState == AssistantState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mic",
                                tint = if (assistantState == AssistantState.LISTENING) Color.White else Color(0xFF021B17)
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for High-Risk Root Commands (e.g. Reboot, Recovery)
    if (confirmationState.isVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmationDialog() },
            title = { Text(confirmationState.title, color = RexyyTextPrimary) },
            text = { Text(confirmationState.message, color = RexyyTextSecondary) },
            confirmButton = {
                Button(
                    onClick = { confirmationState.onConfirm() },
                    colors = ButtonDefaults.buttonColors(containerColor = RexyyWarning)
                ) {
                    Text("Confirm Execution", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmationDialog() }) {
                    Text("Cancel", color = RexyyTextSecondary)
                }
            },
            containerColor = RexyySurface
        )
    }
}
