package com.Ben.closetstylist.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ClaudeRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<ClaudeMessage>,
)

@Serializable
data class ClaudeMessage(
    val role: String,
    val content: List<JsonElement>,
)

@Serializable
data class ClaudeResponse(
    val content: List<ClaudeContent>,
)

@Serializable
data class ClaudeContent(
    val type: String,
    val text: String = "",
)
