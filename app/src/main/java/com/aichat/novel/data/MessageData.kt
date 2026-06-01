package com.aichat.novel.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

// --- Request models ---

data class ChatRequest(
    val model: String,              // "deepseek-v4-pro" or "deepseek-v4-flash"
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    @SerializedName("stream_options")
    val streamOptions: StreamOptions? = StreamOptions(includeUsage = true),
    val thinking: ThinkingConfig? = null,
    @SerializedName("reasoning_effort")
    val reasoningEffort: String? = null  // "high", "medium", "low"
)

data class ThinkingConfig(
    val type: String  // "enabled" or "disabled"
)

data class StreamOptions(
    @SerializedName("include_usage")
    val includeUsage: Boolean = true
)

data class ChatMessage(
    val role: String,               // "user" / "assistant" / "system"
    val content: String
)

// --- Streaming response models ---

data class ChatCompletionChunk(
    val id: String?,
    val choices: List<ChunkChoice>?,
    val usage: Usage?
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)

data class ChunkChoice(
    val delta: DeltaContent?,
    @SerializedName("finish_reason")
    val finishReason: String?       // "stop" or null
)

data class DeltaContent(
    val content: String?,           // text increment
    @SerializedName("reasoning_content")
    val reasoningContent: String?   // thinking increment (reasoner model only)
)

// --- SSE parsing helper ---

private val gson = Gson()

/**
 * Parses a single SSE data line from the DeepSeek streaming API.
 *
 * Line format: "data: {json}"
 * Returns null for empty lines, keep-alive lines, and the "data: [DONE]" sentinel.
 */
fun parseSSELine(line: String): ChatCompletionChunk? {
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return null
    if (!trimmed.startsWith("data: ")) return null

    val json = trimmed.removePrefix("data: ")
    if (json == "[DONE]") return null

    return try {
        gson.fromJson(json, ChatCompletionChunk::class.java)
    } catch (e: Exception) {
        null
    }
}
