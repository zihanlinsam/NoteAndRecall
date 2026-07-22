package com.noteandrecall.util

import com.noteandrecall.data.KnowledgeItem
import java.text.SimpleDateFormat
import java.util.*

object MarkdownUtil {
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    private val displayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    fun exportToMarkdown(items: List<KnowledgeItem>): String {
        val sb = StringBuilder()
        sb.appendLine("---")
        sb.appendLine("note-recall-export: 1")
        sb.appendLine("exported: ${inputFormat.format(Date())}")
        sb.appendLine("count: ${items.size}")
        sb.appendLine("---")
        sb.appendLine()
        sb.appendLine("## Note&Recall Knowledge Export")
        sb.appendLine()

        items.forEachIndexed { index, item ->
            sb.appendLine("---")
            sb.appendLine()
            sb.appendLine("### ${index + 1}. ${item.title}")
            val tagList = item.tags.split(",").filter { it.isNotBlank() }.joinToString(" ") { "#${it.trim()}" }
            sb.appendLine("- **Tags**: $tagList")
            sb.appendLine("- **Recalls**: ${item.recallCount}")
            sb.appendLine("- **Created**: ${displayFormat.format(Date(item.createdAt))} (${item.location.ifBlank { "Unknown" }})")
            sb.appendLine("- **Source**: ${item.source}")
            sb.appendLine()
            sb.appendLine(item.content)
            sb.appendLine()
        }
        sb.appendLine("---")
        return sb.toString()
    }

    fun importFromMarkdown(markdown: String): List<KnowledgeItem> {
        val items = mutableListOf<KnowledgeItem>()
        val blocks = markdown.split("---").filter { it.trim().isNotBlank() }

        for (block in blocks) {
            val lines = block.trim().lines()
            val titleLine = lines.firstOrNull { it.startsWith("### ") } ?: continue
            val title = titleLine.removePrefix("### ").trim()

            var tags = ""
            var recalls = 0
            var createdAt = System.currentTimeMillis()
            var source = "TEXT"
            var contentStartIdx = 0

            for ((i, line) in lines.withIndex()) {
                when {
                    line.startsWith("- **Tags**:") -> {
                        tags = line.removePrefix("- **Tags**:").trim()
                            .split("\\s+".toRegex())
                            .filter { it.startsWith("#") }
                            .joinToString(",") { it.removePrefix("#") }
                    }
                    line.startsWith("- **Recalls**:") -> {
                        recalls = line.removePrefix("- **Recalls**:").trim().toIntOrNull() ?: 0
                    }
                    line.startsWith("- **Created**:") -> {
                        val dateStr = line.removePrefix("- **Created**:").trim().split(" (").firstOrNull() ?: ""
                        try { createdAt = displayFormat.parse(dateStr)?.time ?: System.currentTimeMillis() } catch (_: Exception) {}
                    }
                    line.startsWith("- **Source**:") -> {
                        source = line.removePrefix("- **Source**:").trim()
                    }
                }
                if (line.isBlank() && i > 3) { contentStartIdx = i + 1; break }
            }

            val content = lines.drop(contentStartIdx).joinToString("\n").trim()
            if (content.isNotBlank()) {
                items.add(KnowledgeItem(
                    title = title, content = content, tags = tags,
                    recallCount = recalls, createdAt = createdAt, source = source
                ))
            }
        }
        return items
    }
}
