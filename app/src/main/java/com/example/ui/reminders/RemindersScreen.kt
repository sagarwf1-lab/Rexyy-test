package com.example.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ReminderEntity
import com.example.features.assistant.AssistantViewModel
import com.example.ui.theme.RexyyCardBorder
import com.example.ui.theme.RexyyCyan
import com.example.ui.theme.RexyyDarkBg
import com.example.ui.theme.RexyyEmerald
import com.example.ui.theme.RexyySurface
import com.example.ui.theme.RexyySurfaceVariant
import com.example.ui.theme.RexyyTextMuted
import com.example.ui.theme.RexyyTextPrimary
import com.example.ui.theme.RexyyTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: AssistantViewModel,
    onBack: () -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Reminders & Schedules", color = RexyyTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reminders_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RexyyCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RexyyDarkBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = RexyyCyan,
                contentColor = Color(0xFF021B17),
                modifier = Modifier.testTag("add_reminder_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        },
        containerColor = RexyyDarkBg
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = RexyyDarkBg
        ) {
            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = RexyyTextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Koi scheduled reminder nahi hai",
                            color = RexyyTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Voice se bole: \"Hello REXYY kal 8 baje yaad dilana\"",
                            color = RexyyTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reminders, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onComplete = { viewModel.markReminderCompleted(reminder.id) },
                            onDelete = { viewModel.deleteReminder(reminder.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var reminderTitle by remember { mutableStateOf("") }
        var selectedDelayMinutes by remember { mutableStateOf(10) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Reminder for Sagar Sir", color = RexyyTextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = reminderTitle,
                        onValueChange = { reminderTitle = it },
                        placeholder = { Text("Reminder title...", color = RexyyTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = RexyyTextPrimary,
                            unfocusedTextColor = RexyyTextPrimary,
                            focusedBorderColor = RexyyCyan,
                            unfocusedBorderColor = RexyyCardBorder
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Quick Presets:", color = RexyyTextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            10 to "In 10m",
                            30 to "In 30m",
                            60 to "In 1h",
                            1440 to "Tomorrow"
                        ).forEach { (minutes, label) ->
                            Button(
                                onClick = { selectedDelayMinutes = minutes },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedDelayMinutes == minutes) RexyyCyan else RexyySurfaceVariant,
                                    contentColor = if (selectedDelayMinutes == minutes) Color.Black else RexyyTextPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reminderTitle.isNotBlank()) {
                            val triggerTime = System.currentTimeMillis() + (selectedDelayMinutes * 60 * 1000L)
                            viewModel.addManualReminder(reminderTitle, triggerTime)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RexyyCyan, contentColor = Color(0xFF021B17))
                ) {
                    Text("Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = RexyyTextSecondary)
                }
            },
            containerColor = RexyySurface
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateStr = formatter.format(Date(reminder.triggerTimeEpochMs))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = RexyySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RexyyCardBorder))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (reminder.isCompleted) RexyyEmerald.copy(alpha = 0.2f) else RexyyCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.Alarm,
                    contentDescription = null,
                    tint = if (reminder.isCompleted) RexyyEmerald else RexyyCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    color = if (reminder.isCompleted) RexyyTextMuted else RexyyTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    color = RexyyTextSecondary,
                    fontSize = 12.sp
                )
            }

            if (!reminder.isCompleted) {
                IconButton(onClick = onComplete) {
                    Icon(Icons.Default.Check, contentDescription = "Complete", tint = RexyyEmerald)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RexyyTextMuted)
            }
        }
    }
}
