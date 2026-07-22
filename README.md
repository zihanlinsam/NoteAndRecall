# Note & Recall

[English version](README.md) | [中文版本](README_ch.md)

**A knowledge capture + spaced repetition Android app.**  
Take notes via text, speech, or image — then reinforce them through flashcard-based recall.

📦 **Test data**: `knowledge_test_data.md` (50 general knowledge cards) — import via app menu → *Import / Export*.

---

## Features

### 📝 Note
- **Text input**: Type directly, add title and tags (comma-separated)
- **Speech input**: Tap Speak to record, tap again to stop — audio is transcribed and auto-polished via AI
- **Image capture**: Take a photo or pick from gallery — compressed and analyzed by AI to extract text
- **AI Polish**: Auto-generate title, tags (up to 3, reusing existing ones), and expanded Markdown content

### 🧠 Recall
- Flashcard format: tap to reveal content (Markdown rendered)
- Three scoring levels:
  - **Unfamiliar** (−5) — couldn't remember
  - **Familiar** (+1) — mostly remembered
  - **Learned** (+50) — know it cold
- Customizable penalty/bonus values in Score Settings
- Dedup: items just reviewed are skipped (resets per session)

### 🔍 Knowledge Management
- Browse, filter by tag, and sort (date / recalls / alphabetical)
- Tap to view/edit details; tap tags to edit; long-press content to edit
- **Batch delete**: Long-press to select multiple items, then delete with confirmation
- Export all notes as Markdown; import from same format

### ⚙️ Settings
| Menu item | Function |
|-----------|----------|
| **AI Config** | Set endpoint, API key, and model (default: MiMo v2.5) |
| **Score Settings** | Adjust Familiar bonus & Unfamiliar penalty |
| **🌗 Auto / ☀️ Light / 🌙 Dark** | Cycle theme modes |
| **📖 Help** | Built-in usage guide |
| **📋 Log** | In-app diagnostic logs for debugging |
| **Import / Export** | Backup and restore notes |

---

## Tech Stack

```
Language:     Kotlin
UI:           Jetpack Compose + Material3
Database:     Room (SQLite)
Navigation:   Navigation Compose
Network:      OkHttp + Gson
AI:           MiMo v2.5 (OpenAI-compatible API)
Markdown:     multiplatform-markdown-renderer-m3
```

## Build

```bash
export ANDROID_HOME=$HOME/Android
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/NoteAndRecall-v1.0.apk`

## Data Privacy

All data is stored locally on your device. No data is uploaded to any server except AI API calls you explicitly trigger (transcription, polish, image extraction).

## License

GNU General Public License v3.0 — see [LICENSE](LICENSE) for details.
