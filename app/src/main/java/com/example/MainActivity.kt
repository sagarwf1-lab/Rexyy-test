package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.features.assistant.AssistantViewModel
import com.example.ui.history.HistoryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.reminders.RemindersScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.setup.SetupScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RexyyDarkBg

enum class ScreenState {
    HOME,
    SETTINGS,
    HISTORY,
    REMINDERS
}

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openTab = intent?.getStringExtra("EXTRA_OPEN_TAB")
        val initialScreen = if (openTab == "reminders") ScreenState.REMINDERS else ScreenState.HOME

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = RexyyDarkBg
                ) {
                    val isFirstLaunchDone by viewModel.userPrefs.isFirstLaunchDone.collectAsState()
                    val isApiKeyConfigured = viewModel.keyStoreManager.isApiKeyConfigured()
                    val isTesting by viewModel.isTestingKey.collectAsState()
                    val testResult by viewModel.testKeyResult.collectAsState()

                    var currentScreen by remember { mutableStateOf(initialScreen) }

                    if (!isFirstLaunchDone || !isApiKeyConfigured) {
                        SetupScreen(
                            isTesting = isTesting,
                            testResult = testResult,
                            onTestKey = { key -> viewModel.testApiKey(key) },
                            onContinue = { key -> viewModel.completeSetup(key) }
                        )
                    } else {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                slideInHorizontally { width -> width } togetherWith
                                        slideOutHorizontally { width -> -width }
                            },
                            label = "screen_transition"
                        ) { screen ->
                            when (screen) {
                                ScreenState.HOME -> HomeScreen(
                                    viewModel = viewModel,
                                    onOpenSettings = { currentScreen = ScreenState.SETTINGS },
                                    onOpenHistory = { currentScreen = ScreenState.HISTORY },
                                    onOpenReminders = { currentScreen = ScreenState.REMINDERS }
                                )
                                ScreenState.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = ScreenState.HOME }
                                )
                                ScreenState.HISTORY -> HistoryScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = ScreenState.HOME }
                                )
                                ScreenState.REMINDERS -> RemindersScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = ScreenState.HOME }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
