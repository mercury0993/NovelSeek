package com.aichat.novel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aichat.novel.data.AppPrefs
import com.aichat.novel.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deepThinking by viewModel.deepThinking.collectAsState()
    val systemPrompt by viewModel.systemPrompt.collectAsState()
    val apiKey by AppPrefs.apiKeyFlow(context).collectAsState(initial = null)

    // Dialog states
    var showEditApiKeyDialog by remember { mutableStateOf(false) }
    var showClearApiKeyDialog by remember { mutableStateOf(false) }
    var showEditPromptDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- API Key Section ---
            SectionHeader("API 配置")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "API Key",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = maskApiKey(apiKey),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { showEditApiKeyDialog = true }) {
                            Text("修改")
                        }
                        OutlinedButton(
                            onClick = { showClearApiKeyDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("清空")
                        }
                    }
                }
            }

            // --- Deep Thinking Section ---
            SectionHeader("深度思考")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (deepThinking) "默认状态：开启" else "默认状态：关闭",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // --- System Prompt Section ---
            SectionHeader("系统提示词")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (systemPrompt.isNullOrBlank()) {
                            "未设置"
                        } else {
                            val prompt = systemPrompt!!
                            if (prompt.length > 50) prompt.take(50) + "..." else prompt
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = { showEditPromptDialog = true }) {
                        Text("编辑提示词")
                    }
                }
            }

            // --- About Section ---
            SectionHeader("关于")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "版本：1.0.0",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // --- Edit API Key Dialog ---
    if (showEditApiKeyDialog) {
        var inputKey by remember { mutableStateOf(apiKey ?: "") }
        AlertDialog(
            onDismissRequest = { showEditApiKeyDialog = false },
            title = { Text("修改 API Key") },
            text = {
                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputKey.isNotBlank()) {
                            scope.launch {
                                AppPrefs.setApiKey(context, inputKey.trim())
                            }
                        }
                        showEditApiKeyDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditApiKeyDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // --- Clear API Key Confirmation Dialog ---
    if (showClearApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showClearApiKeyDialog = false },
            title = { Text("清空 API Key") },
            text = { Text("确定要清空已保存的 API Key 吗？清空后需要重新设置才能使用。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearApiKey()
                        showClearApiKeyDialog = false
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearApiKeyDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // --- Edit System Prompt Dialog ---
    if (showEditPromptDialog) {
        var inputPrompt by remember { mutableStateOf(systemPrompt ?: "") }
        AlertDialog(
            onDismissRequest = { showEditPromptDialog = false },
            title = { Text("编辑系统提示词") },
            text = {
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    label = { Text("系统提示词") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateSystemPrompt(inputPrompt.trim())
                        showEditPromptDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPromptDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * Section header text styled with primary color.
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

/**
 * Masks an API key for display:
 * - If key is null or empty, returns a placeholder.
 * - Otherwise shows first 3 chars + bullet dots + last 4 chars.
 */
private fun maskApiKey(key: String?): String {
    if (key.isNullOrBlank()) return "未设置"
    if (key.length <= 7) return "••••••••"
    return key.take(3) + "••••••••" + key.takeLast(4)
}
