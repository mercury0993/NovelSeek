# NovelSeek Android App Design Spec

## Overview

NovelSeek is an Android AI chat app for novel writing users, powered by DeepSeek API. The UI and interaction closely mimic the official DeepSeek mobile client.

**Tech Stack:** Kotlin + Jetpack Compose + MVVM + DataStore + Room + Retrofit + OkHttp  
**Min SDK:** Android 8.0 (API 26)

## Architecture

```
┌─────────────────────────────────────┐
│              UI Layer               │
│  ChatPage / SettingsPage / SetupPage│
│  + ChatBubble / Drawer / Markdown   │
└──────────────┬──────────────────────┘
               │ StateFlow / Events
┌──────────────▼──────────────────────┐
│           ViewModel Layer           │
│  ChatViewModel (聊天逻辑+状态)       │
│  SettingsViewModel (设置逻辑)        │
└──────────┬────────────┬─────────────┘
           │            │
┌──────────▼──┐   ┌─────▼─────────────┐
│  Data Layer │   │   Network Layer   │
│  AppPrefs   │   │   ApiClient       │
│  (DataStore)│   │   DeepSeekApi     │
│             │   │   (Retrofit+OkHttp)│
│  Room DB    │   └───────────────────┘
│  Conversation│
│  Message     │
└─────────────┘
```

## Data Model

### Room Entities

```
Conversation(
  id: String (UUID),
  title: String,          // 取首条消息前20字
  createdAt: Long,
  updatedAt: Long
)

Message(
  id: String (UUID),
  conversationId: String,
  role: String,           // "user" / "assistant" / "system"
  content: String,
  reasoningContent: String?, // 思考过程（仅 reasoner 模型）
  timestamp: Long,
  isThinking: Boolean     // 思考中占位
)
```

### DataStore Keys

- `apiKey: String?` — DeepSeek API Key
- `deepThinking: Boolean` — 深度思考开关，默认 false
- `systemPrompt: String?` — 自定义系统提示词

### API Entities

```
ChatRequest(model, messages, stream=true)
ChatMessage(role, content)
ChatCompletionChunk(choices)
ChunkChoice(delta, finishReason)
DeltaContent(content, reasoning_content)
```

## Navigation

```
App 启动
  ├─ 有 API Key → ChatPage
  │    ├─ 左滑/汉堡菜单 → 侧边抽屉（对话列表）
  │    ├─ 设置按钮 → SettingsPage
  │    └─ 底部输入区
  └─ 无 API Key → ApiKeySetupPage → 保存后 → ChatPage
```

## Pages

### ApiKeySetupPage

- 居中卡片布局，DeepSeek 蓝色主题
- API 地址文本框（只读，默认 `https://api.deepseek.com/v1/chat/completions`）
- API Key 输入框（密码样式，可切换明文）
- 保存按钮（验证非空 → DataStore → 跳转 ChatPage）

### ChatPage

- **顶部栏**：左侧 "NovelSeek"，右侧新建对话 + 设置齿轮
- **侧边抽屉**：对话列表（标题+时间），支持删除，底部"清空所有"
- **深度思考开关**：顶部栏下方 Switch
- **消息列表**：LazyColumn
  - 用户消息右对齐（浅蓝气泡 #E8F0FE）
  - AI 消息左对齐（深灰气泡 #F5F5F5），Markdown 渲染
  - 深度思考模式：思考过程可折叠展示（默认收起，灰色背景）
  - 思考中显示动画加载指示器
- **底部输入区**：多行输入框（最多5行自动扩展）+ 圆形发送按钮 + 重新生成按钮
- 发送后清空输入框，自动滚动到底部
- 加载中禁止重复发送
- 重新生成：删除最后一条 AI 消息，基于上下文重新请求

### SettingsPage

- 返回箭头 + "设置" 标题
- **API 配置**：Key 掩码显示、修改、清空（清空后下次跳配置页）
- **深度思考**：显示当前默认状态
- **系统提示词**：摘要显示，点击进入编辑
- **关于**：版本号

## Color Scheme

| Element | Color |
|---------|-------|
| Primary | #4D6BFE (DeepSeek Blue) |
| Background (light) | #FFFFFF |
| Background (dark) | #1A1A1A |
| User bubble | #E8F0FE |
| AI bubble | #F5F5F5 |
| Text primary | #1A1A1A |
| Text secondary | #666666 |
| Thinking area bg | #F0F0F0 |

## Network Layer

### OkHttp Interceptors

- Auth: dynamically read API Key from DataStore per request
- Content-Type: application/json
- Timeouts: connect 30s, read 120s, write 30s

### Streaming Implementation

1. POST with `stream=true`, get `ResponseBody`
2. Read line by line (`source.readUtf8Line()`)
3. Skip empty lines and `data: [DONE]`
4. Parse each line JSON → `ChatCompletionChunk`
5. Extract `delta.content` and `delta.reasoning_content`
6. StateFlow incrementally appends to current AI message
7. Compose observes StateFlow for live UI updates (typewriter effect)

Direct OkHttp `Call.execute()` on IO coroutine, not Retrofit `@Streaming`.

### Error Handling

| Scenario | Message |
|----------|---------|
| Network unreachable | "网络连接失败，请检查网络" |
| 401 | "API Key 无效，请在设置中修改" |
| 429 | "请求过于频繁，请稍后再试" |
| 500+ | "服务暂时不可用，请稍后重试" |
| Stream interrupted | 保留部分内容，提示"回复中断" |

## ViewModel State

```kotlin
ChatViewModel:
  conversations: StateFlow<List<Conversation>>
  currentMessages: StateFlow<List<Message>>
  currentConversationId: StateFlow<String?>
  isGenerating: StateFlow<Boolean>
  deepThinking: StateFlow<Boolean>
  drawerOpen: StateFlow<Boolean>

  sendMessage(content)
  regenerateLastResponse()
  createNewConversation()
  switchConversation(id)
  deleteConversation(id)
  toggleDeepThinking()
  openDrawer() / closeDrawer()
```

## Dependencies (build.gradle.kts)

```kotlin
// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")
implementation("androidx.activity:activity-compose:1.8.2")
implementation("androidx.navigation:navigation-compose:2.7.7")

// Lifecycle + ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// Retrofit + OkHttp
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Markdown rendering
implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.13.0")

// Gson
implementation("com.google.code.gson:gson:2.10.1")
```

## Project Structure

```
app/src/main/java/com/aichat/novel/
├─ MainActivity.kt
├─ data/
│  ├─ AppPrefs.kt
│  ├─ MessageData.kt
│  └─ db/
│     ├─ AppDatabase.kt
│     ├─ ConversationDao.kt
│     └─ MessageDao.kt
├─ network/
│  ├─ ApiClient.kt
│  └─ DeepSeekApiService.kt
├─ viewmodel/
│  └─ ChatViewModel.kt
└─ ui/
   ├─ ChatPage.kt
   ├─ ApiKeySetupPage.kt
   ├─ SettingsPage.kt
   └─ components/
      └─ ChatBubble.kt
```

## Constraints

- All network/data ops in coroutines, no main thread blocking
- Official DeepSeek API only, no reverse-engineered endpoints
- No rate limiting or cooldown, user's own API quota applies
- Code must compile and run directly in Android Studio
