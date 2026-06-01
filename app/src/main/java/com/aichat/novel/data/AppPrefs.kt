package com.aichat.novel.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

object AppPrefs {

    internal val API_KEY = stringPreferencesKey("api_key")
    private val DEEP_THINKING = booleanPreferencesKey("deep_thinking")
    private val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
    private val SELECTED_MODEL = stringPreferencesKey("selected_model")

    // Available models with their context window sizes
    data class ModelInfo(
        val id: String,
        val name: String,
        val contextWindow: Int,  // in tokens
        val maxOutputTokens: Int
    )

    val AVAILABLE_MODELS = listOf(
        ModelInfo("deepseek-v4-pro", "V4 Pro (1M)", 1_000_000, 65_536),
        ModelInfo("deepseek-v4-flash", "V4 Flash (1M)", 1_000_000, 65_536)
    )

    fun getModelInfo(modelId: String): ModelInfo {
        return AVAILABLE_MODELS.find { it.id == modelId } ?: AVAILABLE_MODELS[0]
    }

    /** Observe the stored API Key (null if not set). */
    fun apiKeyFlow(context: Context): Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[API_KEY]?.ifEmpty { null }
        }

    /** Observe the deep-thinking toggle (defaults to false). */
    fun deepThinkingFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[DEEP_THINKING] ?: false
        }

    /** Observe the custom system prompt (null if not set). */
    fun systemPromptFlow(context: Context): Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[SYSTEM_PROMPT]?.ifEmpty { null }
        }

    /** Observe the selected model (defaults to deepseek-v4-pro). */
    fun selectedModelFlow(context: Context): Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[SELECTED_MODEL] ?: "deepseek-v4-pro"
        }

    /** Save the DeepSeek API Key. */
    suspend fun setApiKey(context: Context, apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[API_KEY] = apiKey
        }
    }

    /** Save the deep-thinking toggle. */
    suspend fun setDeepThinking(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DEEP_THINKING] = enabled
        }
    }

    /** Save the custom system prompt. */
    suspend fun setSystemPrompt(context: Context, prompt: String) {
        context.dataStore.edit { prefs ->
            prefs[SYSTEM_PROMPT] = prompt
        }
    }

    /** Clear the stored API Key. */
    suspend fun clearApiKey(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(API_KEY)
        }
    }

    /** Save the selected model. */
    suspend fun setSelectedModel(context: Context, modelId: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_MODEL] = modelId
        }
    }
}
