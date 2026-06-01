package com.aichat.novel.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aichat.novel.data.AppPrefs
import com.aichat.novel.ui.ApiKeySetupPage
import com.aichat.novel.ui.ChatPage
import com.aichat.novel.ui.SettingsPage
import com.aichat.novel.viewmodel.ChatViewModel
import kotlinx.coroutines.flow.first

/**
 * Root navigation graph for NovelSeek.
 *
 * On first launch (no API key saved) the user is directed to the setup page.
 * On subsequent launches the chat page is shown immediately.
 */
@Composable
fun NovelSeekNavGraph(viewModel: ChatViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Determine start destination based on whether an API key is stored.
    // Use a loading gate so we don't flash the wrong screen before DataStore is ready.
    var loading by remember { mutableStateOf(true) }
    var startDestination by remember { mutableStateOf("chat") }

    LaunchedEffect(Unit) {
        val key = AppPrefs.apiKeyFlow(context).first()
        startDestination = if (key.isNullOrBlank()) "setup" else "chat"
        loading = false
    }

    if (loading) {
        // Simple loading indicator while DataStore initializes
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("setup") {
            ApiKeySetupPage(
                onNavigateToChat = {
                    navController.navigate("chat") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }
        composable("chat") {
            ChatPage(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsPage(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
