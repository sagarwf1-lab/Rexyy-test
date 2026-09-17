package com.example.core.ai

data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

interface AiProvider {
    val providerName: String
    suspend fun testConnection(apiKey: String): Result<String>
    suspend fun generateResponse(
        apiKey: String,
        model: String,
        conversationHistory: List<ChatMessage>,
        userPrompt: String
    ): Result<String>
}
