package com.noteandrecall.util

import com.noteandrecall.data.KnowledgeItem
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

data class GraphNode(
    val id: Long,
    val title: String,
    val tags: List<String>,
    val x: Float,
    val y: Float,
    val parentTag: String = ""
)

data class GraphTagNode(
    val tag: String,
    val colorIndex: Int,
    val x: Float,
    val y: Float,
    val count: Int = 1,
    val radius: Float = 35f
)

/**
 * Compute a solar-system-style layout for the knowledge graph.
 *
 * - Tags are sorted by occurrence frequency (descending).
 * - Only the top 25 tags are shown.
 * - Tags are placed on concentric rings: most frequent → innermost ring.
 * - Tag circle diameter is linearly proportional to occurrence count.
 * - Item nodes (knowledge cards) orbit their parent tag.
 */
fun computeLayout(items: List<KnowledgeItem>): Pair<List<GraphTagNode>, List<GraphNode>> {
    if (items.isEmpty()) return Pair(emptyList(), emptyList())

    // 1. Count tag occurrences across all items
    val tagCounts = mutableMapOf<String, Int>()
    val tagItemMap = mutableMapOf<String, MutableList<KnowledgeItem>>()
    for (item in items) {
        val itemTags = item.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val tags = if (itemTags.isEmpty()) listOf("Untagged") else itemTags
        for (tag in tags) {
            tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
            tagItemMap.getOrPut(tag) { mutableListOf() }.add(item)
        }
    }

    // 2. Sort tags by frequency (descending), take top 25
    val sortedTags = tagCounts.entries
        .sortedByDescending { it.value }
        .take(25)
        .map { it.key }

    val visibleCount = sortedTags.size
    if (visibleCount == 0) return Pair(emptyList(), emptyList())

    // 3. Frequency stats for radius scaling
    val maxFreq = tagCounts.values.maxOrNull() ?: 1
    val minFreq = sortedTags.mapNotNull { tagCounts[it] }.minOrNull() ?: 1
    val freqRange = (maxFreq - minFreq).coerceAtLeast(1)

    // 4. Assign rings using floor(sqrt(rank))
    //    Ring 0: rank 0        (1 tag)
    //    Ring 1: rank 1-3      (3 tags)
    //    Ring 2: rank 4-8      (5 tags)
    //    Ring 3: rank 9-15     (7 tags)
    //    Ring 4: rank 16-24    (9 tags)
    data class RingInfo(val index: Int, val tags: MutableList<String>)
    val rings = mutableMapOf<Int, RingInfo>()
    for ((rank, tag) in sortedTags.withIndex()) {
        val ringIdx = floor(sqrt(rank.toDouble())).toInt()
        rings.getOrPut(ringIdx) { RingInfo(ringIdx, mutableListOf()) }.tags.add(tag)
    }

    // 5. Build tag nodes with concentric ring positions
    val baseRadius = 80f
    val ringStep = 130f
    val minTagRadius = 20f
    val maxTagRadius = 55f

    val tagNodes = mutableListOf<GraphTagNode>()
    val itemNodes = mutableListOf<GraphNode>()

    for ((ringIdx, ringInfo) in rings.toSortedMap()) {
        val ringRadius = baseRadius + ringIdx * ringStep
        val tagsInRing = ringInfo.tags
        val n = tagsInRing.size
        if (n == 0) continue

        for ((i, tag) in tagsInRing.withIndex()) {
            val angle = 2.0 * PI * i / n - PI / 2.0  // start from top
            val freq = tagCounts[tag] ?: 1
            val t = ((freq - minFreq).toFloat() / freqRange).coerceIn(0f, 1f)
            val tagRadius = minTagRadius + t * (maxTagRadius - minTagRadius)

            val tx = ringRadius * cos(angle).toFloat()
            val ty = ringRadius * sin(angle).toFloat()

            tagNodes.add(
                GraphTagNode(
                    tag = tag,
                    colorIndex = sortedTags.indexOf(tag) % 12,
                    x = tx,
                    y = ty,
                    count = freq,
                    radius = tagRadius
                )
            )

            // 6. Place item nodes (knowledge cards) orbiting their parent tag
            val groupItems = tagItemMap[tag] ?: emptyList()
            val itemCount = groupItems.size
            if (itemCount == 0) continue

            val itemOrbitRadius = tagRadius + 80f + 10f * sqrt(itemCount.toFloat().coerceAtLeast(1f))
            for ((itemIdx, item) in groupItems.withIndex()) {
                // Offset each item's angle slightly to spread them around the tag
                val offsetAngle = if (itemCount == 1) 0.0 else 2.0 * PI * itemIdx / itemCount
                val ix = tx + itemOrbitRadius * cos(offsetAngle).toFloat()
                val iy = ty + itemOrbitRadius * sin(offsetAngle).toFloat()

                itemNodes.add(
                    GraphNode(
                        id = item.id,
                        title = item.title,
                        tags = item.tags.split(",").map { it.trim() },
                        x = ix,
                        y = iy,
                        parentTag = tag
                    )
                )
            }
        }
    }

    return Pair(tagNodes, itemNodes)
}
