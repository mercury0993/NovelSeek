# NovelSeek Android App Implementation Plan

> **Goal:** Build an Android AI chat app for novel writers using DeepSeek API, mimicking the official DeepSeek mobile UI.

> **Architecture:** Single Activity + Compose Navigation, MVVM with ViewModel/StateFlow, Room for conversation persistence, DataStore for preferences, Retrofit+OkHttp for API.

> **Tech Stack:** Kotlin, Jetpack Compose, Material3, Room, DataStore, Retrofit, OkHttp, Gson

---

## File Map

```
app/
├─ build.gradle.kts
├─ src/main/
│  ├─ AndroidManifest.xml
│  └─ java/com/aichat/novel/
│     ├─ MainActivity.kt
│     ├─ NovelSeekApp.kt              # Application class
│     ├─ data/
│     │  ├─ AppPrefs.kt               # DataStore wrapper
│     │  ├─ MessageData.kt            # API request/response models
│     │  └─ db/
│     │     ├─ AppDatabase.kt         # Room database
│     │     ├─ ConversationEntity.kt
│     │     ├─ MessageEntity.kt
│     │     ├─ ConversationDao.kt
│     │     └─ MessageDao.kt
│     ├─ network/
│     │  ├─ ApiClient.kt              # Retrofit+OkHttp singleton
│     │  └─ DeepSeekApiService.kt     # API interface
│     ├─ viewmodel/
│     │  └─ ChatViewModel.kt
│     └─ ui/
│        ├─ theme/
│        │  └─ Theme.kt               # Colors, Typography
│        ├─ navigation/
│        │  └─ NavGraph.kt            # Compose Navigation
│        ├─ ChatPage.kt
│        ├─ ApiKeySetupPage.kt
│        ├─ SettingsPage.kt
│        └─ components/
│           └─ ChatBubble.kt
```

---

## Tasks

### Task 1: Project Scaffolding
- Create Android project structure (app module, gradle config)
- `app/build.gradle.kts` with all dependencies
- `AndroidManifest.xml` with INTERNET permission
- `NovelSeekApp.kt` Application class
- `MainActivity.kt` skeleton with Compose

### Task 2: Data Layer — DataStore
- `AppPrefs.kt`: DataStore wrapper for apiKey, deepThinking, systemPrompt
- Read/write flows using Preferences DataStore

### Task 3: Data Layer — Room Database
- `ConversationEntity.kt`, `MessageEntity.kt` entities
- `ConversationDao.kt`, `MessageDao.kt` DAOs with Flow queries
- `AppDatabase.kt` Room database singleton

### Task 4: Data Layer — API Models
- `MessageData.kt`: ChatRequest, ChatMessage, ChatCompletionChunk, ChunkChoice, DeltaContent

### Task 5: Network Layer
- `ApiClient.kt`: OkHttp with auth interceptor (reads API Key from DataStore per request)
- `DeepSeekApiService.kt`: Retrofit interface

### Task 6: Theme & Common UI
- `Theme.kt`: DeepSeek blue color scheme, Material3 typography
- `ChatBubble.kt`: User/AI bubble components with Markdown support

### Task 7: ViewModel
- `ChatViewModel.kt`: StateFlow for conversations, messages, generating state, deep thinking toggle
- sendMessage(), regenerateLastResponse(), createNewConversation(), switchConversation(), deleteConversation()
- Streaming response handling via OkHttp + coroutine

### Task 8: ApiKeySetupPage
- Centered card layout, API address (read-only), Key input (password toggle), save button
- Navigate to ChatPage on save

### Task 9: ChatPage
- Top bar with app name, new chat button, settings icon
- Deep thinking toggle switch
- Message list (LazyColumn) with auto-scroll
- Bottom input area with send button and regenerate button
- Side drawer with conversation list, delete gesture

### Task 10: SettingsPage
- API Key display (masked), modify, clear
- Deep thinking status display
- System prompt editor
- Back navigation

### Task 11: Navigation & Integration
- `NavGraph.kt`: Compose Navigation routes (Setup, Chat, Settings)
- `MainActivity.kt`: Check API Key on startup, set initial route
- Wire everything together, test full flow

---

## Execution Order
Tasks 1-5 (data+network) → Task 6 (theme+UI components) → Task 7 (ViewModel) → Tasks 8-10 (pages) → Task 11 (integration)
