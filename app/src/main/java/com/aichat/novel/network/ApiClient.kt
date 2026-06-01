package com.aichat.novel.network

import com.aichat.novel.NovelSeekApp
import com.aichat.novel.data.AppPrefs
import com.aichat.novel.data.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    /** Shared OkHttpClient — exposed so [DeepSeekStreamClient] can create raw calls. */
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)   // long timeout for streaming
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            // Auth interceptor: read API key from DataStore synchronously
            val context = NovelSeekApp.instance.applicationContext
            val apiKey = runBlocking {
                context.dataStore.data.first()[AppPrefs.API_KEY] ?: ""
            }
            chain.proceed(
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()
            )
        }
        .addInterceptor { chain ->
            // Content-Type interceptor
            chain.proceed(
                chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .build()
            )
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.deepseek.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /** Lazily-created Retrofit service for non-streaming API calls. */
    val apiService: DeepSeekApiService by lazy {
        retrofit.create(DeepSeekApiService::class.java)
    }
}
