package com.example.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class OpenAiClient : AiProvider {

    override val providerName: String = "OpenAI"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun testConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API key cannot be empty."))
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/models")
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> Result.success("Connection successful! API key is valid.")
                    401 -> Result.failure(Exception("API key invalid hai. Settings mein API key check karo."))
                    429 -> Result.failure(Exception("OpenAI quota exceed ho gaya hai ya rate limit reach ho gayi hai."))
                    else -> Result.failure(Exception("OpenAI connection failed with status ${response.code}."))
                }
            }
        } catch (e: UnknownHostException) {
            Result.failure(Exception("Internet connection nahi hai, Sagar Sir."))
        } catch (e: IOException) {
            Result.failure(Exception("Network timeout ya connection error: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateResponse(
        apiKey: String,
        model: String,
        conversationHistory: List<ChatMessage>,
        userPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("API key missing hai. Setup ya Settings mein API key daalo."))
        }

        try {
            val messagesArray = JSONArray()

            // System prompt instructing REXYY identity and tone
            val systemObj = JSONObject().apply {
                put("role", "system")
                put(
                    "content",
                    "You are REXYY, an ultra-fast, intelligent, Jarvis-inspired personal AI voice assistant created for Sagar Sir. " +
                    "Always address the user with respect as 'Sagar Sir'. " +
                    "Your conversational style is friendly, professional, crisp Hinglish (natural Hindi + English). " +
                    "You effortlessly understand and answer in Hindi, English, Hinglish, Urdu, and Bengali. " +
                    "Keep your spoken answers direct, concise, natural, and conversational (1 to 3 sentences maximum unless Sagar Sir explicitly requests a long explanation, essay, or code). " +
                    "Never hallucinate or pretend to execute device actions you cannot perform directly."
                )
            }
            messagesArray.put(systemObj)

            // Conversation history (last 8 messages for context window)
            val recentHistory = conversationHistory.takeLast(8)
            for (msg in recentHistory) {
                messagesArray.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }

            // Current prompt
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })

            val requestBodyJson = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("messages", messagesArray)
                put("max_tokens", 350)
                put("temperature", 0.7)
            }

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .header("Content-Type", "application/json")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@use when (response.code) {
                        401 -> Result.failure(Exception("API key invalid hai. Settings mein API key check karo."))
                        429 -> Result.failure(Exception("API rate limit ya quota error, Sagar Sir."))
                        else -> Result.failure(Exception("AI request error (${response.code})"))
                    }
                }

                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val message = firstChoice.getJSONObject("message")
                    val content = message.getString("content").trim()
                    Result.success(content)
                } else {
                    Result.failure(Exception("AI ne empty response diya."))
                }
            }
        } catch (e: UnknownHostException) {
            Result.failure(Exception("Internet connection nahi hai, Sagar Sir."))
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.localizedMessage}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
