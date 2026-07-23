package com.noteandrecall.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.noteandrecall.data.KnowledgeDao
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
    var showItemLabels by remember { mutableStateOf(true) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    val (tagNodes, itemNodes) = remember(items) { computeLayout(items) }
    val density = LocalDensity.current.density
    val tagFontBase = 14f * density
    val tagFontMin = 10f * density
    val tagFontMax = 22f * density
    val itemFontBase = 10f * density
    val itemFontMin = 9f * density
    val itemFontMax = 15f * density
    val textColor = MaterialTheme.colorScheme.onBackground
    val textColorInt = android.graphics.Color.rgb(
        (textColor.red * 255).toInt(),
        (textColor.green * 255).toInt(),
        (textColor.blue * 255).toInt()
    )

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(itemNodes) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                // Center the tap point at 2x zoom
                                val newScale = scale * 2f
                                val ratio = newScale / scale
                                offsetX = ratio * offsetX + (1f - ratio) * (tapOffset.x - cx)
                                offsetY = ratio * offsetY + (1f - ratio) * (tapOffset.y - cy)
                                scale = newScale
                            },
                            onTap = { tapOffset ->
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
                        )
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

                // Draw tag nodes + labels (always visible)
                for (tagNode in tagNodes) {
                    val sx = cx + (tagNode.x * scale) + offsetX
                    val sy = cy + (tagNode.y * scale) + offsetY
                    val color = graphColors[tagNode.colorIndex % graphColors.size]
                    val radius = tagNode.radius * scale

                    drawCircle(color = color.copy(alpha = 0.6f), radius = radius, center = Offset(sx, sy))
                    drawCircle(color = color, radius = radius, center = Offset(sx, sy), style = Stroke(width = 3f * scale))

                    paint.textSize = (tagFontBase * scale).coerceIn(tagFontMin, tagFontMax)
                    paint.color = android.graphics.Color.rgb(
                        (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt()
                    )
                    paint.textAlign = android.graphics.Paint.Align.CENTER
                    drawContext.canvas.nativeCanvas.drawText(tagNode.tag, sx, sy + paint.textSize / 3f, paint)
                }

                // Draw item nodes + title
                for (item in itemNodes) {
                    val sx = cx + (item.x * scale) + offsetX
                    val sy = cy + (item.y * scale) + offsetY
                    val ir = 7f * scale

                    // Item node circle in theme-aware color
                    drawCircle(color = textColor, radius = ir, center = Offset(sx, sy))

                    // Item label with toggle
                    if (showItemLabels && scale > 0.4f) {
                        val display = if (item.title.length > 6) item.title.take(6) + "..." else item.title
                        paint.textSize = (itemFontBase * scale).coerceIn(itemFontMin, itemFontMax)
                        paint.color = textColorInt
                        paint.textAlign = android.graphics.Paint.Align.CENTER
                        drawContext.canvas.nativeCanvas.drawText(display, sx, sy - ir - 4f, paint)
                    }
                }
            }

            // Bottom toggle for card titles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = showItemLabels,
                    onCheckedChange = { showItemLabels = it }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Show card titles",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
