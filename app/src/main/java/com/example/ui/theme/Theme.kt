package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RexyyCyan,
    onPrimary = Color(0xFF021B17),
    primaryContainer = Color(0xFF003831),
    onPrimaryContainer = RexyyCyan,
    secondary = RexyyBlue,
    onSecondary = Color(0xFF001B2E),
    tertiary = RexyyEmerald,
    background = RexyyDarkBg,
    onBackground = RexyyTextPrimary,
    surface = RexyySurface,
    onSurface = RexyyTextPrimary,
    surfaceVariant = RexyySurfaceVariant,
    onSurfaceVariant = RexyyTextSecondary,
    outline = RexyyCardBorder,
    error = RexyyError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark/futuristic theme as requested by prompt
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
