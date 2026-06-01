package com.aichat.novel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aichat.novel.ui.navigation.NovelSeekNavGraph
import com.aichat.novel.ui.theme.NovelSeekTheme
import com.aichat.novel.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NovelSeekTheme {
                val viewModel: ChatViewModel = viewModel()
                NovelSeekNavGraph(viewModel)
            }
        }
    }
}
