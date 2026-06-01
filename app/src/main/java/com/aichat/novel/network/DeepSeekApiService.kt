package com.aichat.novel.network

import com.aichat.novel.data.ChatCompletionChunk
import com.aichat.novel.data.ChatRequest
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Body
import retrofit2.http.POST

interface DeepSeekApiService {

    @POST("v1/chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatCompletionChunk
}

/**
 * Direct OkHttp-based streaming client for DeepSeek SSE responses.
 *
 * Uses the shared [ApiClient.okHttpClient] so that auth and content-type
 * interceptors are applied automatically.  The returned [Call] can be
 * executed and its response body consumed line-by-line for SSE streaming.
 */
object DeepSeekStreamClient {

    fun streamChat(request: ChatRequest): Call {
        val json = Gson().toJson(request)
        val body = json.toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("https://api.deepseek.com/v1/chat/completions")
            .post(body)
            .build()
        return ApiClient.okHttpClient.newCall(req)
    }
}
