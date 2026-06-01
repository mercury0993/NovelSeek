package com.aichat.novel.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aichat.novel.ui.theme.NovelSeekColors
import com.mikepenz.markdown.m3.Markdown

/**
 * Right-aligned user chat bubble with light blue background.
 */
@Composable
fun UserBubble(content: String) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * 0.8f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 8.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .background(NovelSeekColors.UserBubble)
                .padding(12.dp)
        ) {
            Text(
                text = content,
                color = NovelSeekColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Left-aligned AI chat bubble with light gray background.
 * Optionally includes a collapsible "thinking process" section.
 */
@Composable
fun AssistantBubble(content: String, reasoningContent: String? = null) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * 0.85f

    var isReasoningExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .clip(
                    RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .background(NovelSeekColors.AiBubble)
                .padding(12.dp)
        ) {
            // Collapsible reasoning section
            if (reasoningContent != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NovelSeekColors.ThinkingBackground)
                        .clickable { isReasoningExpanded = !isReasoningExpanded }
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "思考过程",
                            color = NovelSeekColors.TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isReasoningExpanded) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = if (isReasoningExpanded) "Collapse" else "Expand",
                            tint = NovelSeekColors.TextSecondary
                        )
                    }
                    AnimatedVisibility(visible = isReasoningExpanded) {
                        Text(
                            text = reasoningContent,
                            color = NovelSeekColors.TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Main content rendered as Markdown
            Markdown(
                content = content,
                modifier = Modifier.padding(top = if (reasoningContent != null) 8.dp else 0.dp)
            )
        }
    }
}
