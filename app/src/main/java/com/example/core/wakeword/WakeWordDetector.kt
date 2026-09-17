package com.example.core.wakeword

data class WakeWordMatch(
    val isWakeWordDetected: Boolean,
    val extractedCommand: String = ""
)

object WakeWordDetector {

    private val WAKE_PATTERNS = listOf(
        Regex("(?i)\\b(?:hello|hey|hai|suno|ok|hi|namaste)\\s+rexyy\\b"),
        Regex("(?i)\\brexyy\\b"),
        Regex("(?i)\\brexy\\b"),
        Regex("(?i)\\braxxy\\b"),
        Regex("(?i)\\brxi\\b")
    )

    fun checkWakeWord(spokenText: String): WakeWordMatch {
        val trimmed = spokenText.trim()
        if (trimmed.isBlank()) return WakeWordMatch(false)

        for (pattern in WAKE_PATTERNS) {
            val match = pattern.find(trimmed)
            if (match != null) {
                val afterMatch = trimmed.substring(match.range.last + 1).trim()
                // Clean punctuation at start of command
                val cleanCommand = afterMatch.trimStart(',', '.', ':', ';', '-', ' ')
                return WakeWordMatch(
                    isWakeWordDetected = true,
                    extractedCommand = cleanCommand
                )
            }
        }

        return WakeWordMatch(false)
    }
}
