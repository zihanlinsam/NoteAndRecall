package com.noteandrecall.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavController
import com.noteandrecall.data.KnowledgeDao
import com.noteandrecall.util.GraphNode
import com.noteandrecall.util.GraphTagNode
import com.noteandrecall.util.computeLayout

private val graphColors = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047),
    Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF00ACC1),
    Color(0xFFD81B60), Color(0xFF6D4C41), Color(0xFF3949AB),
    Color(0xFF00897B), Color(0xFFFDD835), Color(0xFF546E7A)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(navController: NavController, dao: KnowledgeDao) {
    val items by dao.getAllItems().collectAsState(initial = emptyList())
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.2f, 5f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    val (tagNodes, itemNodes) = remember(items) { computeLayout(items) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Knowledge Graph") },
                navigationIcon = {
                    IconButton(onClick = { if (navController.previousBackStackEntry != null) navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(itemNodes) {
                    detectTapGestures { tapOffset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        for (node in itemNodes) {
                            val sx = cx + (node.x * scale) + offsetX
                            val sy = cy + (node.y * scale) + offsetY
                            val dist = kotlin.math.sqrt(
                                (tapOffset.x - sx) * (tapOffset.x - sx) +
                                (tapOffset.y - sy) * (tapOffset.y - sy)
                            )
                            if (dist < 30f * scale) {
                                navController.navigate("detail/${node.id}")
                                return@detectTapGestures
                            }
                        }
                    }
                }
                .transformable(state = transformableState)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val tagColorMap = tagNodes.associate { it.tag to graphColors[it.colorIndex % graphColors.size] }
            val paint = android.graphics.Paint().apply { isAntiAlias = true }

            // Draw lines: item → tag
            for (item in itemNodes) {
                val tag = item.tags.firstOrNull() ?: continue
                val parent = tagNodes.find { it.tag == tag } ?: continue
                val color = tagColorMap[tag] ?: graphColors[0]
                drawLine(
                    color = color.copy(alpha = 0.3f),
                    start = Offset(cx + (item.x * scale) + offsetX, cy + (item.y * scale) + offsetY),
                    end = Offset(cx + (parent.x * scale) + offsetX, cy + (parent.y * scale) + offsetY),
                    strokeWidth = 1.5f * scale
                )
            }

            // Draw tag nodes + labels
            for (tagNode in tagNodes) {
                val sx = cx + (tagNode.x * scale) + offsetX
                val sy = cy + (tagNode.y * scale) + offsetY
                val color = graphColors[tagNode.colorIndex % graphColors.size]
                val radius = 35f * scale

                drawCircle(color = color.copy(alpha = 0.2f), radius = radius, center = Offset(sx, sy))
                drawCircle(color = color, radius = radius, center = Offset(sx, sy), style = Stroke(width = 3f * scale))

                if (scale > 0.5f) {
                    paint.textSize = (14f * scale).coerceIn(10f, 20f)
                    paint.color = color.hashCode()
                    paint.color = android.graphics.Color.rgb(
                        (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt()
                    )
                    paint.textAlign = android.graphics.Paint.Align.CENTER
                    drawContext.canvas.nativeCanvas.drawText(tagNode.tag, sx, sy + paint.textSize / 3f, paint)
                }
            }

            // Draw item nodes + title
            for (item in itemNodes) {
                val sx = cx + (item.x * scale) + offsetX
                val sy = cy + (item.y * scale) + offsetY
                val tag = item.tags.firstOrNull() ?: ""
                val color = tagColorMap[tag] ?: graphColors[0]
                val ir = 7f * scale

                drawCircle(color = color, radius = ir, center = Offset(sx, sy))

                if (scale > 0.8f) {
                    val display = if (item.title.length > 5) item.title.take(5) + "..." else item.title
                    paint.textSize = (9f * scale).coerceIn(8f, 14f)
                    paint.color = android.graphics.Color.rgb(
                        (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt()
                    )
                    paint.textAlign = android.graphics.Paint.Align.CENTER
                    drawContext.canvas.nativeCanvas.drawText(display, sx, sy - ir - 3f, paint)
                }
            }
        }
    }
}
