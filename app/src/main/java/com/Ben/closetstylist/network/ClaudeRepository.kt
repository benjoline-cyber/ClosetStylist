package com.Ben.closetstylist.network

import android.util.Base64
import com.Ben.closetstylist.data.SettingsRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class ClaudeRepository(private val settingsRepository: SettingsRepository) {

    private val json = Json { ignoreUnknownKeys = true }

    private val service: ClaudeApiService = Retrofit.Builder()
        .baseUrl("https://api.anthropic.com/")
        .client(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .addHeader("x-api-key", settingsRepository.claudeApiKey.value)
                            .addHeader("anthropic-version", "2023-06-01")
                            .build(),
                    )
                }
                .build(),
        )
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ClaudeApiService::class.java)

    suspend fun describeItem(imageBytes: ByteArray): String {
        check(settingsRepository.claudeApiKey.value.isNotEmpty()) {
            "Claude API key is not set"
        }
        val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val imageBlock = buildJsonObject {
            put("type", "image")
            putJsonObject("source") {
                put("type", "base64")
                put("media_type", "image/jpeg")
                put("data", base64)
            }
        }
        val textBlock = buildJsonObject {
            put("type", "text")
            put("text", DESCRIBE_PROMPT)
        }
        val request = ClaudeRequest(
            model = "claude-haiku-4-5-20251001",
            maxTokens = 150,
            messages = listOf(ClaudeMessage(role = "user", content = listOf(imageBlock, textBlock))),
        )
        return service.createMessage(request)
            .content
            .firstOrNull { it.type == "text" }
            ?.text
            ?.trim()
            ?: ""
    }

    suspend fun suggestOutfits(prompt: String, inspirationPhotos: List<ByteArray>): String {
        check(settingsRepository.claudeApiKey.value.isNotEmpty()) {
            "Claude API key not set"
        }
        val content = buildList {
            inspirationPhotos.take(3).forEach { bytes ->
                add(
                    buildJsonObject {
                        put("type", "image")
                        putJsonObject("source") {
                            put("type", "base64")
                            put("media_type", "image/jpeg")
                            put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
                        }
                    },
                )
            }
            add(
                buildJsonObject {
                    put("type", "text")
                    put("text", prompt)
                },
            )
        }
        val request = ClaudeRequest(
            model = "claude-haiku-4-5-20251001",
            maxTokens = 1024,
            messages = listOf(ClaudeMessage(role = "user", content = content)),
        )
        return service.createMessage(request)
            .content
            .firstOrNull { it.type == "text" }
            ?.text
            ?.trim()
            ?: ""
    }

    suspend fun testConnection(): Boolean {
        val key = settingsRepository.claudeApiKey.value
        if (key.isEmpty()) return false
        val request = ClaudeRequest(
            model = "claude-haiku-4-5-20251001",
            maxTokens = 5,
            messages = listOf(
                ClaudeMessage(
                    role = "user",
                    content = listOf(buildJsonObject { put("type", "text"); put("text", "Reply: OK") }),
                ),
            ),
        )
        return runCatching { service.createMessage(request) }.isSuccess
    }

    companion object {
        private const val DESCRIBE_PROMPT =
            "Describe this clothing item in one sentence covering type, color, pattern, and material if visible. No preamble."
    }
}
