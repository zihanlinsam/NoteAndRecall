# Note & Recall — 中文版

[English version](README.md) | [中文版本](README_ch.md)

**一款知识采集 + 间隔重复回忆的 Android 应用。**  
通过文字、语音或图片记录笔记，再通过闪卡式回忆强化记忆。

📦 **测试数据**：`knowledge_test_data.md`（50 条通用知识卡片）— 通过应用菜单 → *Import / Export* 导入。

---

## 功能

### 📝 记录
- **文字输入**：直接输入，添加标题和标签（逗号分隔）
- **语音输入**：点击 Speak 开始录音，再次点击停止——音频通过 AI 转写并自动润色
- **图片采集**：拍照或从相册选择——压缩后由 AI 分析提取文字
- **AI 润色**：自动生成标题、标签（最多 3 个，优先复用已有），扩展为 Markdown 内容

### 🧠 回忆
- 闪卡形式：点击卡片显示内容（Markdown 渲染）
- 三个评分等级：
  - **不熟悉**（−5）— 没记住
  - **熟悉**（+1）— 基本记住
  - **学会了**（+50）— 已经掌握
- 可自定义罚分/奖励值（评分设置）
- 去重：刚回顾的条目跳过（每次重新进入回忆时重置）

### 🔍 知识管理
- 按标签筛选、按日期/回忆次数/字母排序
- 点击查看/编辑详情；点击标签可编辑；长按内容可编辑
- **批量删除**：长按选择多条，确认后批量删除
- 导出所有笔记为 Markdown；从相同格式导入

### ⚙️ 设置
| 菜单项 | 功能 |
|--------|------|
| **AI 配置** | 设置端点、API Key 和模型（默认：MiMo v2.5） |
| **评分设置** | 调整熟悉奖励和不熟悉罚分 |
| **🌗 自动 / ☀️ 浅色 / 🌙 深色** | 循环切换主题模式 |
| **📖 帮助** | 内置使用指南 |
| **📋 日志** | 应用内诊断日志，用于调试 |
| **导入 / 导出** | 备份和恢复笔记 |

---

## 技术栈

```
语言：       Kotlin
UI：         Jetpack Compose + Material3
数据库：     Room (SQLite)
导航：       Navigation Compose
网络：       OkHttp + Gson
AI：         MiMo v2.5（兼容 OpenAI 的 API）
Markdown：   multiplatform-markdown-renderer-m3
```

## 构建

```bash
export ANDROID_HOME=$HOME/Android
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/NoteAndRecall-v1.0.apk`

## 数据隐私

所有数据存储在你的设备本地。除了你明确触发的 AI API 调用（转写、润色、图片提取）外，不会上传任何数据到服务器。

## 许可证

GNU General Public License v3.0 — 详见 [LICENSE](LICENSE)。
