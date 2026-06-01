package com.aichat.novel.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aichat.novel.data.AppPrefs
import com.aichat.novel.ui.components.AssistantBubble
import com.aichat.novel.ui.components.UserBubble
import com.aichat.novel.ui.theme.NovelSeekColors
import com.aichat.novel.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Formats token count to human-readable string (e.g., 1.2K, 128K, 1M)
 */
private fun formatTokenCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000 -> "${count / 1_000}K"
        else -> "$count"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPage(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit
) {
    // Collect ViewModel state
    val conversations by viewModel.conversations.collectAsState()
    val currentMessages by viewModel.currentMessages.collectAsState()
    val currentConversationId by viewModel.currentConversationId.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val deepThinking by viewModel.deepThinking.collectAsState()
    val lastTokenUsage by viewModel.lastTokenUsage.collectAsState()
    val contextTokenCount by viewModel.contextTokenCount.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    // Local UI state
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Date formatter for drawer items
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(currentMessages.size) {
        if (currentMessages.isNotEmpty()) {
            listState.animateScrollToItem(currentMessages.size - 1)
        }
        // Update estimated context tokens when messages change
        viewModel.updateEstimatedContextTokens()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(LocalConfiguration.current.screenWidthDp.dp * 0.75f)
            ) {
                // Drawer header
                Text(
                    text = "对话历史",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )

                HorizontalDivider()

                // Conversation list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(conversations, key = { it.id }) { conversation ->
                        val isCurrent = conversation.id == currentConversationId
                        var showDeleteDialog by remember { mutableStateOf(false) }

                        NavigationDrawerItem(
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = conversation.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = dateFormat.format(Date(conversation.updatedAt)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NovelSeekColors.TextSecondary
                                        )
                                    }
                                    IconButton(
                                        onClick = { showDeleteDialog = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "删除对话",
                                            modifier = Modifier.size(18.dp),
                                            tint = NovelSeekColors.TextSecondary
                                        )
                                    }
                                }
                            },
                            selected = isCurrent,
                            onClick = {
                                viewModel.switchConversation(conversation.id)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        // Delete confirmation dialog
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("删除对话") },
                                text = { Text("确定要删除「${conversation.title}」吗？") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.deleteConversation(conversation.id)
                                        showDeleteDialog = false
                                    }) {
                                        Text("删除")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) {
                                        Text("取消")
                                    }
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Clear all button
                var showClearAllDialog by remember { mutableStateOf(false) }
                TextButton(
                    onClick = { showClearAllDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("清空所有对话")
                }

                if (showClearAllDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearAllDialog = false },
                        title = { Text("清空所有对话") },
                        text = { Text("确定要删除所有对话吗？此操作不可撤销。") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.deleteAllConversations()
                                showClearAllDialog = false
                                scope.launch { drawerState.close() }
                            }) {
                                Text("清空", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearAllDialog = false }) {
                                Text("取消")
                            }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("NovelSeek") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "菜单"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.createNewConversation() }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "新对话"
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "设置"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                // Model selection and deep thinking toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Model selector dropdown
                    var modelMenuExpanded by remember { mutableStateOf(false) }
                    val currentModelInfo = AppPrefs.getModelInfo(selectedModel)

                    Box {
                        TextButton(
                            onClick = { modelMenuExpanded = true }
                        ) {
                            Text(
                                text = currentModelInfo.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NovelSeekColors.Primary
                            )
                        }

                        DropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false }
                        ) {
                            AppPrefs.AVAILABLE_MODELS.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(model.name)
                                            Text(
                                                text = "上下文: ${formatTokenCount(model.contextWindow)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = NovelSeekColors.TextSecondary
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.switchModel(model.id)
                                        modelMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Deep thinking toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "深度思考",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NovelSeekColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = deepThinking,
                            onCheckedChange = { viewModel.toggleDeepThinking() }
                        )
                    }
                }

                // Token usage display
                if (lastTokenUsage != null || contextTokenCount > 0) {
                    val currentModelInfo = AppPrefs.getModelInfo(selectedModel)
                    val contextPercentage = if (contextTokenCount > 0 && currentModelInfo.contextWindow > 0) {
                        (contextTokenCount.toFloat() / currentModelInfo.contextWindow * 100).toInt()
                    } else 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Last API call token usage
                        lastTokenUsage?.let { usage ->
                            Text(
                                text = "上次: ${formatTokenCount(usage.totalTokens)} tokens",
                                style = MaterialTheme.typography.bodySmall,
                                color = NovelSeekColors.TextSecondary
                            )
                        }

                        // Context token count with percentage
                        if (contextTokenCount > 0) {
                            Text(
                                text = "上下文: ${formatTokenCount(contextTokenCount)}/${formatTokenCount(currentModelInfo.contextWindow)} ($contextPercentage%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (contextPercentage > 80) MaterialTheme.colorScheme.error else NovelSeekColors.TextSecondary
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Message list
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (currentMessages.isEmpty()) {
                        // Empty state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "开始新的对话吧！",
                                style = MaterialTheme.typography.bodyLarge,
                                color = NovelSeekColors.TextSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(currentMessages, key = { it.id }) { message ->
                                when {
                                    message.isThinking -> {
                                        // Thinking indicator
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(
                                                        RoundedCornerShape(
                                                            topStart = 8.dp,
                                                            topEnd = 16.dp,
                                                            bottomStart = 16.dp,
                                                            bottomEnd = 16.dp
                                                        )
                                                    )
                                                    .background(NovelSeekColors.AiBubble)
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = NovelSeekColors.Primary
                                                )
                                                Text(
                                                    text = "思考中...",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = NovelSeekColors.TextSecondary
                                                )
                                            }
                                        }
                                    }
                                    message.role == "user" -> {
                                        UserBubble(content = message.content)
                                    }
                                    message.role == "assistant" -> {
                                        AssistantBubble(
                                            content = message.content,
                                            reasoningContent = message.reasoningContent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Input area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "输入消息...",
                                color = NovelSeekColors.TextSecondary
                            )
                        },
                        maxLines = 5,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovelSeekColors.Primary,
                            unfocusedBorderColor = NovelSeekColors.TextSecondary.copy(alpha = 0.3f)
                        )
                    )

                    // Regenerate button (only visible when there are messages and not generating)
                    AnimatedVisibility(visible = currentMessages.isNotEmpty() && !isGenerating) {
                        IconButton(
                            onClick = { viewModel.regenerateLastResponse() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "重新生成",
                                tint = NovelSeekColors.TextSecondary
                            )
                        }
                    }

                    // Send button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isGenerating) {
                                viewModel.sendMessage(inputText.trim())
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isGenerating)
                                    NovelSeekColors.Primary
                                else
                                    NovelSeekColors.TextSecondary.copy(alpha = 0.3f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = if (inputText.isNotBlank() && !isGenerating)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                NovelSeekColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
