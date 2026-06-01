package com.aichat.novel.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.novel.NovelSeekApp
import com.aichat.novel.data.AppPrefs
import com.aichat.novel.data.ChatMessage
import com.aichat.novel.data.ChatRequest
import com.aichat.novel.data.ThinkingConfig
import com.aichat.novel.data.Usage
import com.aichat.novel.data.db.ConversationDao
import com.aichat.novel.data.db.ConversationEntity
import com.aichat.novel.data.db.MessageDao
import com.aichat.novel.data.db.MessageEntity
import com.aichat.novel.data.parseSSELine
import com.aichat.novel.network.DeepSeekStreamClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = NovelSeekApp.instance.database
    private val conversationDao: ConversationDao = db.conversationDao()
    private val messageDao: MessageDao = db.messageDao()

    // --- StateFlows exposed to UI ---

    private val _conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val conversations: StateFlow<List<ConversationEntity>> = _conversations.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val currentMessages: StateFlow<List<MessageEntity>> = _currentMessages.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _deepThinking = MutableStateFlow(false)
    val deepThinking: StateFlow<Boolean> = _deepThinking.asStateFlow()

    private val _drawerOpen = MutableStateFlow(false)
    val drawerOpen: StateFlow<Boolean> = _drawerOpen.asStateFlow()

    private val _systemPrompt = MutableStateFlow<String?>(null)
    val systemPrompt: StateFlow<String?> = _systemPrompt.asStateFlow()

    private val _selectedModel = MutableStateFlow("deepseek-v4-pro")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    // Token usage tracking
    private val _lastTokenUsage = MutableStateFlow<TokenUsage?>(null)
    val lastTokenUsage: StateFlow<TokenUsage?> = _lastTokenUsage.asStateFlow()

    private val _contextTokenCount = MutableStateFlow(0)
    val contextTokenCount: StateFlow<Int> = _contextTokenCount.asStateFlow()

    companion object {
        private const val DEFAULT_SYSTEM_PROMPT =
            "你是一位专业的小说创作助手，帮助用户构思情节、塑造角色、润色文笔。请用中文回复。"
    }

    data class TokenUsage(
        val promptTokens: Int,
        val completionTokens: Int,
        val totalTokens: Int
    )

    init {
        // Collect conversations from Room, sorted by updatedAt desc
        viewModelScope.launch {
            conversationDao.getAll().collect { list ->
                _conversations.value = list
            }
        }

        // Collect deepThinking preference from DataStore
        viewModelScope.launch {
            AppPrefs.deepThinkingFlow(context).collect { enabled ->
                _deepThinking.value = enabled
            }
        }

        // Collect systemPrompt preference from DataStore
        viewModelScope.launch {
            AppPrefs.systemPromptFlow(context).collect { prompt ->
                _systemPrompt.value = prompt
            }
        }

        // Collect selectedModel preference from DataStore
        viewModelScope.launch {
            AppPrefs.selectedModelFlow(context).collect { model ->
                _selectedModel.value = model
            }
        }

        // Load most recent conversation if exists
        viewModelScope.launch {
            val latest = _conversations.value.firstOrNull()
            if (latest != null) {
                switchConversation(latest.id)
            }
        }
    }

    // --- User actions ---

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        if (_isGenerating.value) return

        viewModelScope.launch {
            // Step 1: Create conversation if needed
            var convId = _currentConversationId.value
            try {
                if (convId == null) {
                    convId = UUID.randomUUID().toString()
                    val title = if (content.length > 20) content.take(20) + "..." else content
                    val now = System.currentTimeMillis()
                    val conversation = ConversationEntity(
                        id = convId,
                        title = title,
                        createdAt = now,
                        updatedAt = now
                    )
                    conversationDao.insert(conversation)
                    _currentConversationId.value = convId
                }

                // Step 2: Create and insert user message
                val userMessage = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    role = "user",
                    content = content,
                    reasoningContent = null,
                    timestamp = System.currentTimeMillis()
                )
                messageDao.insert(userMessage)
                // Update local list immediately
                _currentMessages.value = _currentMessages.value + userMessage

                // Step 3: Call API
                callApi(convId)

            } catch (e: Exception) {
                handleError(e, convId)
            }
        }
    }

    fun regenerateLastResponse() {
        if (_isGenerating.value) return
        val convId = _currentConversationId.value ?: return

        viewModelScope.launch {
            try {
                // Get last message
                val lastMsg = messageDao.getLastMessage(convId) ?: return@launch
                if (lastMsg.role == "assistant") {
                    // Delete last assistant message
                    messageDao.deleteLastAssistantMessage(convId)
                    // Update local list
                    _currentMessages.value = _currentMessages.value.dropLast(1)
                }

                // Re-request from API
                callApi(convId)

            } catch (e: Exception) {
                handleError(e, convId)
            }
        }
    }

    fun createNewConversation() {
        _currentConversationId.value = null
        _currentMessages.value = emptyList()
    }

    fun switchConversation(id: String) {
        _currentConversationId.value = id

        // Collect messages for this conversation
        viewModelScope.launch {
            messageDao.getByConversation(id).collect { messages ->
                // Only update if still on the same conversation
                if (_currentConversationId.value == id) {
                    _currentMessages.value = messages
                }
            }
        }
    }

    fun deleteAllConversations() {
        viewModelScope.launch {
            conversationDao.deleteAll()
            _currentConversationId.value = null
            _currentMessages.value = emptyList()
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            // Delete conversation (Room CASCADE will delete messages)
            conversationDao.delete(id)

            // If we deleted the active conversation, clear it
            if (_currentConversationId.value == id) {
                _currentConversationId.value = null
                _currentMessages.value = emptyList()

                // Switch to the most recent remaining conversation
                val remaining = _conversations.value.firstOrNull { it.id != id }
                if (remaining != null) {
                    switchConversation(remaining.id)
                }
            }
        }
    }

    fun toggleDeepThinking() {
        val newValue = !_deepThinking.value
        _deepThinking.value = newValue
        viewModelScope.launch {
            AppPrefs.setDeepThinking(context, newValue)
        }
    }

    fun openDrawer() {
        _drawerOpen.value = true
    }

    fun closeDrawer() {
        _drawerOpen.value = false
    }

    fun updateSystemPrompt(prompt: String) {
        _systemPrompt.value = prompt
        viewModelScope.launch {
            AppPrefs.setSystemPrompt(context, prompt)
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            AppPrefs.clearApiKey(context)
        }
    }

    fun switchModel(modelId: String) {
        _selectedModel.value = modelId
        viewModelScope.launch {
            AppPrefs.setSelectedModel(context, modelId)
        }
    }

    /**
     * Estimates token count for a given text.
     * Rough estimation: Chinese ~1.5 chars/token, English ~4 chars/token
     */
    private fun estimateTokenCount(text: String): Int {
        if (text.isEmpty()) return 0
        var count = 0
        for (char in text) {
            count += if (char.code > 127) 1 else 1 // CJK characters
        }
        // Rough estimate: ~1.5 tokens per Chinese character, ~0.25 tokens per English char
        val chineseChars = text.count { it.code > 127 }
        val englishChars = text.length - chineseChars
        return (chineseChars * 1.5 + englishChars * 0.25).toInt()
    }

    /**
     * Updates the estimated context token count based on current messages.
     */
    fun updateEstimatedContextTokens() {
        val messages = _currentMessages.value
        val systemPrompt = _systemPrompt.value ?: DEFAULT_SYSTEM_PROMPT
        var totalTokens = estimateTokenCount(systemPrompt)

        for (message in messages) {
            if (!message.isThinking) {
                totalTokens += estimateTokenCount(message.content)
                if (message.reasoningContent != null) {
                    totalTokens += estimateTokenCount(message.reasoningContent)
                }
            }
        }

        _contextTokenCount.value = totalTokens
    }

    // --- Private helpers ---

    /**
     * Core API call flow: builds context, streams response, updates messages.
     */
    private suspend fun callApi(conversationId: String) {
        _isGenerating.value = true

        try {
            // Build ChatMessage list from current messages (excluding thinking placeholder)
            val contextMessages = _currentMessages.value
                .filter { !it.isThinking }
                .map { ChatMessage(role = it.role, content = it.content) }

            // Prepend system message
            val systemContent = _systemPrompt.value ?: DEFAULT_SYSTEM_PROMPT
            val systemMessage = ChatMessage(role = "system", content = systemContent)
            val allMessages = listOf(systemMessage) + contextMessages

            // Use selected model (user can choose any model including V4 series)
            val model = _selectedModel.value

            // Configure thinking mode based on deepThinking toggle
            val thinkingConfig = if (_deepThinking.value) {
                ThinkingConfig(type = "enabled")
            } else {
                ThinkingConfig(type = "disabled")
            }

            // Create request
            val request = ChatRequest(
                model = model,
                messages = allMessages,
                stream = true,
                thinking = thinkingConfig,
                reasoningEffort = if (_deepThinking.value) "high" else null
            )

            // Execute streaming call on IO dispatcher
            val result = withContext(Dispatchers.IO) {
                val call = DeepSeekStreamClient.streamChat(request)
                val response = call.execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    val errorMsg = when (response.code) {
                        401 -> "API Key 无效，请检查设置"
                        429 -> "请求过于频繁，请稍后再试"
                        in 500..599 -> "服务器错误 (${response.code})，请稍后再试"
                        else -> "请求失败 (${response.code}): $errorBody"
                    }
                    return@withContext StreamResult.Error(errorMsg)
                }

                val source = response.body?.source()
                    ?: return@withContext StreamResult.Error("响应为空")

                val accumulatedContent = StringBuilder()
                val accumulatedReasoning = StringBuilder()
                var thinkingId: String? = null
                var lastUsage: Usage? = null

                try {
                    var line: String?
                    while (source.readUtf8Line().also { line = it } != null) {
                        val currentLine = line ?: continue
                        if (currentLine.trim() == "data: [DONE]") break

                        val chunk = parseSSELine(currentLine) ?: continue
                        val choice = chunk.choices?.firstOrNull() ?: continue
                        val delta = choice.delta ?: continue

                        // Accumulate reasoning content
                        if (delta.reasoningContent != null) {
                            accumulatedReasoning.append(delta.reasoningContent)

                            // Show/update thinking message in list
                            val tid = thinkingId ?: UUID.randomUUID().toString().also { thinkingId = it }
                            val thinkingMsg = MessageEntity(
                                id = tid,
                                conversationId = conversationId,
                                role = "assistant",
                                content = "",
                                reasoningContent = accumulatedReasoning.toString(),
                                timestamp = System.currentTimeMillis(),
                                isThinking = true
                            )
                            _currentMessages.value =
                                _currentMessages.value.filter { it.id != tid } + thinkingMsg
                        }

                        // Accumulate regular content
                        if (delta.content != null) {
                            accumulatedContent.append(delta.content)

                            // Once content starts arriving, update message with content
                            // If we had a thinking message, flip isThinking to false
                            val msgId = thinkingId ?: UUID.randomUUID().toString().also { thinkingId = it }
                            val assistantMsg = MessageEntity(
                                id = msgId,
                                conversationId = conversationId,
                                role = "assistant",
                                content = accumulatedContent.toString(),
                                reasoningContent = accumulatedReasoning.toString().ifEmpty { null },
                                timestamp = System.currentTimeMillis(),
                                isThinking = false
                            )
                            _currentMessages.value =
                                _currentMessages.value.filter { it.id != msgId } + assistantMsg
                        }

                        // Capture usage information
                        if (chunk.usage != null) {
                            lastUsage = chunk.usage
                        }
                    }
                } finally {
                    source.close()
                }

                // Build final message for persistence
                val finalMessage = MessageEntity(
                    id = thinkingId ?: UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = "assistant",
                    content = accumulatedContent.toString(),
                    reasoningContent = accumulatedReasoning.toString().ifEmpty { null },
                    timestamp = System.currentTimeMillis(),
                    isThinking = false,
                    promptTokens = lastUsage?.promptTokens,
                    completionTokens = lastUsage?.completionTokens,
                    totalTokens = lastUsage?.totalTokens
                )

                StreamResult.Success(finalMessage, lastUsage)
            }

            when (result) {
                is StreamResult.Success -> {
                    // Insert final message into Room
                    messageDao.insert(result.message)

                    // Replace temporary message in local list with persisted one
                    _currentMessages.value = _currentMessages.value
                        .filter { it.id != result.message.id } + result.message

                    // Update token usage
                    if (result.usage != null) {
                        _lastTokenUsage.value = TokenUsage(
                            promptTokens = result.usage.promptTokens,
                            completionTokens = result.usage.completionTokens,
                            totalTokens = result.usage.totalTokens
                        )
                        // Update context token count (prompt tokens represent the context)
                        _contextTokenCount.value = result.usage.promptTokens
                    }

                    // Update conversation title if this was the first user message
                    val userMsgCount = _currentMessages.value.count { it.role == "user" }
                    if (userMsgCount == 1) {
                        val firstUserMsg = _currentMessages.value.firstOrNull { it.role == "user" }
                        if (firstUserMsg != null) {
                            val title = if (firstUserMsg.content.length > 20)
                                firstUserMsg.content.take(20) + "..." else firstUserMsg.content
                            conversationDao.updateTitle(conversationId, title)
                        }
                    }
                }
                is StreamResult.Error -> {
                    // Show error as assistant message
                    val errorMsg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        role = "assistant",
                        content = "错误: ${result.message}",
                        reasoningContent = null,
                        timestamp = System.currentTimeMillis()
                    )
                    messageDao.insert(errorMsg)
                    _currentMessages.value = _currentMessages.value + errorMsg
                }
            }
        } catch (e: Exception) {
            handleError(e, conversationId)
        } finally {
            _isGenerating.value = false
        }
    }

    /**
     * Handles exceptions caught during sendMessage or regenerateLastResponse.
     */
    private suspend fun handleError(e: Exception, conversationId: String? = null) {
        _isGenerating.value = false

        val errorDescription = when (e) {
            is java.net.UnknownHostException -> "网络连接失败，请检查网络"
            is java.net.SocketTimeoutException -> "请求超时，请稍后再试"
            is java.io.IOException -> "网络错误: ${e.localizedMessage}"
            else -> "未知错误: ${e.localizedMessage ?: e.javaClass.simpleName}"
        }

        // Show error as assistant message if we have a conversation
        if (conversationId != null) {
            val errorMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "assistant",
                content = "错误: $errorDescription",
                reasoningContent = null,
                timestamp = System.currentTimeMillis()
            )
            messageDao.insert(errorMsg)
            _currentMessages.value = _currentMessages.value + errorMsg
        }
    }

    /**
     * Sealed class for streaming result handling.
     */
    private sealed class StreamResult {
        data class Success(val message: MessageEntity, val usage: Usage? = null) : StreamResult()
        data class Error(val message: String) : StreamResult()
    }
}
