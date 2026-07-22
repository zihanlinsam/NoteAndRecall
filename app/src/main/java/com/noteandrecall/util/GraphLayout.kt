package com.noteandrecall.util

import com.noteandrecall.data.KnowledgeItem
import kotlin.math.cos
import kotlin.math.sin

data class GraphNode(
    val id: Long,
    val title: String,
    val tags: List<String>,
    val x: Float,
    val y: Float
)

data class GraphTagNode(
    val tag: String,
    val colorIndex: Int,
    val x: Float,
    val y: Float
)

fun computeLayout(items: List<KnowledgeItem>): Pair<List<GraphTagNode>, List<GraphNode>> {
    if (items.isEmpty()) return Pair(emptyList(), emptyList())

    // Group items by tags
    val tagGroups = mutableMapOf<String, MutableList<KnowledgeItem>>()
    for (item in items) {
        val itemTags = item.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (itemTags.isEmpty()) {
            tagGroups.getOrPut("Untagged") { mutableListOf() }.add(item)
        } else {
            for (tag in itemTags) {
                tagGroups.getOrPut(tag) { mutableListOf() }.add(item)
            }
        }
    }

    val tagNames = tagGroups.keys.sorted()
    val tagCount = tagNames.size
    val radius = 300f * kotlin.math.sqrt(tagCount.toFloat().coerceAtLeast(1f))

    val tagNodes = mutableListOf<GraphTagNode>()
    val itemNodes = mutableListOf<GraphNode>()

    for ((tagIdx, tag) in tagNames.withIndex()) {
        val angle = 2.0 * Math.PI * tagIdx / tagCount
        val tagX = radius * cos(angle).toFloat()
        val tagY = radius * sin(angle).toFloat()

        val tagNode = GraphTagNode(
            tag = tag,
            colorIndex = tagIdx % 12,
            x = tagX,
            y = tagY
        )
        tagNodes.add(tagNode)

        val groupItems = tagGroups[tag] ?: emptyList()
        val itemCount = groupItems.size
        if (itemCount == 0) continue

        val itemRadius = 80f * kotlin.math.sqrt(itemCount.toFloat().coerceAtLeast(1f))
        for ((itemIdx, item) in groupItems.withIndex()) {
            val itemAngle = 2.0 * Math.PI * itemIdx / itemCount
            val ix = tagX + itemRadius * cos(itemAngle).toFloat()
            val iy = tagY + itemRadius * sin(itemAngle).toFloat()

            val itemNode = GraphNode(
                id = item.id,
                title = item.title,
                tags = item.tags.split(",").map { it.trim() },
                x = ix,
                y = iy
            )
            itemNodes.add(itemNode)
        }
    }

    return Pair(tagNodes, itemNodes)
}
