# NovelSeek 项目状态

> 最后更新：2026-06-01

## 项目概述

NovelSeek 是一款面向小说创作用户的 Android AI 聊天应用，对接 DeepSeek V4 API (1M 上下文)，UI 风格模仿 DeepSeek 官方移动端。

**技术栈：** Kotlin + Jetpack Compose + MVVM + DataStore + Room + Retrofit + OkHttp
**最低兼容：** Android 8.0 (API 26)
**支持模型：** DeepSeek V4 Pro (1M)、DeepSeek V4 Flash (1M)
**API 兼容：** OpenAI API 格式，支持 thinking 和 reasoning_effort 参数
**GitHub：** https://github.com/mercury0993/NovelSeek

## 实现状态：全部完成 ✅

### 已完成的任务

| # | 任务 | 状态 | 文件 |
|---|------|------|------|
| 1 | 项目脚手架 | ✅ | build.gradle.kts, settings.gradle.kts, AndroidManifest.xml, NovelSeekApp.kt, MainActivity.kt |
| 2 | DataStore | ✅ | data/AppPrefs.kt |
| 3 | Room 数据库 | ✅ | data/db/ConversationEntity.kt, MessageEntity.kt, ConversationDao.kt, MessageDao.kt, AppDatabase.kt |
| 4 | API 模型 | ✅ | data/MessageData.kt |
| 5 | 网络层 | ✅ | network/ApiClient.kt, DeepSeekApiService.kt |
| 6 | 主题 & UI 组件 | ✅ | ui/theme/Theme.kt, ui/components/ChatBubble.kt |
| 7 | ViewModel | ✅ | viewmodel/ChatViewModel.kt |
| 8 | API Key 配置页 | ✅ | ui/ApiKeySetupPage.kt |
| 9 | 聊天主页 | ✅ | ui/ChatPage.kt |
| 10 | 设置页 | ✅ | ui/SettingsPage.kt |
| 11 | 导航集成 | ✅ | ui/navigation/NavGraph.kt, MainActivity.kt (更新) |

## 项目文件结构

```
E:/lx/projects/NovelSeek/
├─ build.gradle.kts                          # 项目级 Gradle (AGP 8.2.2, Kotlin 1.9.22)
├─ settings.gradle.kts                       # 仓库配置
├─ gradle.properties                         # Android 属性
├─ .gitignore                                # Git 忽略规则
├─ docs/
│  ├─ superpowers/specs/...                  # 设计文档
│  └─ superpowers/plans/...                  # 实现计划
└─ app/
   ├─ build.gradle.kts                       # 依赖配置
   └─ src/main/
      ├─ AndroidManifest.xml                 # INTERNET 权限
      ├─ res/
      │  ├─ drawable/                        # 矢量图标（回退）
      │  ├─ mipmap-mdpi/                     # 48x48 图标
      │  ├─ mipmap-hdpi/                     # 72x72 图标
      │  ├─ mipmap-xhdpi/                    # 96x96 图标
      │  ├─ mipmap-xxhdpi/                   # 144x144 图标
      │  ├─ mipmap-xxxhdpi/                  # 192x192 图标
      │  └─ values/                          # 主题、颜色定义
      └─ java/com/aichat/novel/
         ├─ MainActivity.kt                  # 主入口，Compose Navigation
         ├─ NovelSeekApp.kt                  # Application，Room 初始化
         ├─ data/
         │  ├─ AppPrefs.kt                   # DataStore (apiKey, deepThinking, systemPrompt, selectedModel)
         │  ├─ MessageData.kt                # API 请求/响应模型 + SSE 解析 + Usage 追踪
         │  └─ db/
         │     ├─ AppDatabase.kt             # Room 数据库 + 迁移
         │     ├─ ConversationEntity.kt      # 对话实体
         │     ├─ MessageEntity.kt           # 消息实体
         │     ├─ ConversationDao.kt         # 对话 DAO
         │     └─ MessageDao.kt              # 消息 DAO
         ├─ network/
         │  ├─ ApiClient.kt                  # Retrofit + OkHttp (Auth 拦截器)
         │  └─ DeepSeekApiService.kt         # API 接口 + SSE 流式客户端
         ├─ viewmodel/
         │  └─ ChatViewModel.kt              # 核心业务逻辑，StateFlow 状态管理
         └─ ui/
            ├─ theme/
            │  └─ Theme.kt                   # DeepSeek 蓝色主题
            ├─ navigation/
            │  └─ NavGraph.kt                # Compose Navigation 路由
            ├─ ApiKeySetupPage.kt            # 首次配置页
            ├─ ChatPage.kt                   # 聊天主页 (抽屉+消息+输入)
            ├─ SettingsPage.kt               # 设置页
            └─ components/
               └─ ChatBubble.kt              # 用户/AI 气泡组件
```

## 核心功能

- **首次启动**：无 API Key → 跳转配置页，保存后进入聊天
- **聊天**：多轮上下文对话，流式输出（逐字效果），Markdown 渲染
- **深度思考**：开关切换 thinking 模式（thinking: enabled/disabled），思考过程可折叠展示
- **多对话管理**：侧边抽屉，创建/切换/删除对话，Room 持久化
- **重新生成**：删除最后一条 AI 回复，基于上下文重新请求
- **设置**：查看/修改/清空 API Key，编辑系统提示词
- **Token 使用量显示**：实时显示每次 API 调用的 token 消耗（prompt/completion/total）
- **上下文占用显示**：估算并显示当前对话的上下文 token 占用量及百分比
- **模型选择**：支持 DeepSeek V4 Pro (1M) 和 V4 Flash (1M) 模型切换
- **API 兼容**：符合 DeepSeek 官方 API 规范，支持 thinking 和 reasoning_effort 参数
- **自定义图标**：透明背景 PNG 图标，各分辨率适配（mdpi ~ xxxhdpi）
- **复制功能**：AI 回复右下角复制按钮，一键复制到剪贴板

## 下一步（在 Android Studio 中）

1. 用 Android Studio 打开 `E:/lx/projects/NovelSeek`
2. 等待 Gradle 同步完成
3. 连接设备或启动模拟器
4. Build & Run
5. 测试完整流程：配置 Key → 发消息 → 深度思考 → 多对话切换

## 构建与发布

- **GitHub 仓库**：https://github.com/mercury0993/NovelSeek
- **最新发行版**：https://github.com/mercury0993/NovelSeek/releases/tag/v1.0.0
- **构建方式**：Android Studio → Build → Build APK(s)
- **APK 输出**：`app/build/outputs/apk/debug/app-debug.apk`
- **签名密钥**：已在 .gitignore 中排除（*.jks, *.keystore）

## 注意事项

- 需要有效的 DeepSeek API Key 才能使用
- 流式输出依赖网络稳定性
- Markdown 渲染使用 mikepenz multiplatform-markdown-renderer 库
- 所有网络/数据操作均在协程中执行，不阻塞主线程
- 自定义图标已生成各分辨率 PNG（mdpi ~ xxxhdpi）
