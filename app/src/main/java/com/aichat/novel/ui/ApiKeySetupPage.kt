package com.aichat.novel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aichat.novel.data.AppPrefs
import com.aichat.novel.ui.theme.NovelSeekColors
import kotlinx.coroutines.launch

/**
 * First-time setup page shown when no API key is saved.
 * User enters their DeepSeek API key which is persisted to DataStore.
 */
@Composable
fun ApiKeySetupPage(
    onNavigateToChat: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var apiKey by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "NovelSeek",
                style = MaterialTheme.typography.headlineLarge,
                color = NovelSeekColors.Primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "AI 小说创作助手",
                style = MaterialTheme.typography.bodyLarge,
                color = NovelSeekColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            // API Address field (read-only)
            Text(
                text = "API 地址",
                style = MaterialTheme.typography.bodyMedium,
                color = NovelSeekColors.TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )

            OutlinedTextField(
                value = "https://api.deepseek.com/v1/chat/completions",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    disabledTextColor = NovelSeekColors.TextPrimary,
                    disabledBorderColor = Color(0xFFE0E0E0),
                    disabledContainerColor = Color(0xFFF5F5F5)
                ),
                enabled = false
            )

            Spacer(modifier = Modifier.height(24.dp))

            // API Key field (password mode with visibility toggle)
            Text(
                text = "API Key",
                style = MaterialTheme.typography.bodyMedium,
                color = NovelSeekColors.TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Filled.Visibility
                            } else {
                                Icons.Filled.VisibilityOff
                            },
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                        )
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Save button
            Button(
                onClick = {
                    if (apiKey.isBlank()) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("请输入 API Key")
                        }
                    } else {
                        coroutineScope.launch {
                            AppPrefs.setApiKey(context, apiKey.trim())
                            onNavigateToChat()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NovelSeekColors.Primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "保  存",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
