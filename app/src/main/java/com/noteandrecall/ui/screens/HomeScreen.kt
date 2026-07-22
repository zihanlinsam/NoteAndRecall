package com.noteandrecall.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.noteandrecall.data.KnowledgeDao
import com.noteandrecall.data.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    prefsManager: PreferencesManager,
    dao: KnowledgeDao
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val themeMode by prefsManager.themeMode.collectAsState(initial = 0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note&Recall") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        val modeLabels = listOf("🌗 Auto", "☀️ Light", "🌙 Dark")
                        DropdownMenuItem(
                            text = { Text(modeLabels[themeMode]) },
                            onClick = { menuExpanded = false; scope.launch { prefsManager.setThemeMode((themeMode + 1) % 3) } }
                        )
                        DropdownMenuItem(
                            text = { Text("📋 Knowledge List") },
                            onClick = { menuExpanded = false; navController.navigate("knowledge_list") }
                        )
                        DropdownMenuItem(
                            text = { Text("📊 Graph") },
                            onClick = { menuExpanded = false; navController.navigate("graph") }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("⚙️ AI Config") },
                            onClick = { menuExpanded = false; navController.navigate("ai_config") }
                        )
                        DropdownMenuItem(
                            text = { Text("⭐ Score Settings") },
                            onClick = { menuExpanded = false; navController.navigate("score_settings") }
                        )
                        DropdownMenuItem(
                            text = { Text("📤 Import / Export") },
                            onClick = { menuExpanded = false; navController.navigate("import_export") }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("📖 Help") },
                            onClick = { menuExpanded = false; navController.navigate("help") }
                        )
                        DropdownMenuItem(
                            text = { Text("ℹ️ About") },
                            onClick = { menuExpanded = false; navController.navigate("about") }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("📋 Log") },
                            onClick = { menuExpanded = false; navController.navigate("log") }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Note card — swipe left for Manual, swipe right for Auto
            var swipeOffset by remember { mutableStateOf(0f) }
            var isTouchActive by remember { mutableStateOf(false) }
            var isSwipeConfirmed by remember { mutableStateOf(false) }
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (isTouchActive && !isSwipeConfirmed) 1f else 0f,
                animationSpec = tween(150)
            )
            val swipeThreshold = 100f
            val swipePhase = remember(swipeOffset) {
                when {
                    swipeOffset >= swipeThreshold -> 1  // Auto
                    swipeOffset <= -swipeThreshold -> -1 // Manual
                    else -> 0  // Default
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isTouchActive = true
                            isSwipeConfirmed = false
                            val startX = down.position.x

                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    if (swipeOffset >= swipeThreshold) {
                                        navController.navigate("note_auto")
                                        swipeOffset = swipeThreshold
                                    } else if (swipeOffset <= -swipeThreshold) {
                                        navController.navigate("note")
                                        swipeOffset = -swipeThreshold
                                    } else {
                                        swipeOffset = 0f
                                    }
                                    isTouchActive = false
                                    isSwipeConfirmed = false
                                    break
                                }
                                val dx = change.position.x - startX
                                swipeOffset = dx.coerceIn(-swipeThreshold, swipeThreshold)
                                if (kotlin.math.abs(swipeOffset) >= swipeThreshold) {
                                    isSwipeConfirmed = true
                                } else {
                                    isSwipeConfirmed = false
                                }
                                change.consume()
                            } while (true)
                        }
                    },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Center content
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        AnimatedContent(targetState = swipePhase, label = "note_swipe") { phase ->
                            when (phase) {
                                1 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("\uD83E\uDD16", fontSize = 40.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Auto Mode", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    Text("AI generates cards",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                                }
                                -1 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("\u270D\uFE0F", fontSize = 40.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Manual Mode", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    Text("Write your own notes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                                }
                                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("\uD83D\uDCDD", fontSize = 40.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Note", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    Text("Swipe left Manual \u00B7 right Auto",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }

                    // Left touch indicator: arrow + pencil icon
                    if (indicatorAlpha > 0.001f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.2f)
                                .alpha(indicatorAlpha)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f * indicatorAlpha)
                                ),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                Text("\u27E8", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = indicatorAlpha))
                                Spacer(Modifier.width(8.dp))
                                Text("\u270D\uFE0F", fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = indicatorAlpha))
                            }
                        }
                    }

                    // Right touch indicator: robot icon + arrow
                    if (indicatorAlpha > 0.001f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.2f)
                                .align(Alignment.CenterEnd)
                                .alpha(indicatorAlpha)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f * indicatorAlpha)
                                ),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text("\uD83E\uDD16", fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = indicatorAlpha))
                                Spacer(Modifier.width(8.dp))
                                Text("\u27E9", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = indicatorAlpha))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate("recall") },
                modifier = Modifier.fillMaxWidth().height(140.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83E\uDDE0", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Recall", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Review what you've learned", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f))
                }
            }
        }
    }
}
