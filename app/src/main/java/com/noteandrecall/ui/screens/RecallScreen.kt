package com.noteandrecall.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mikepenz.markdown.m3.Markdown
import com.noteandrecall.data.KnowledgeDao
import com.noteandrecall.data.KnowledgeItem
import com.noteandrecall.data.PreferencesManager
import com.noteandrecall.util.VibrationUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecallScreen(
    navController: NavController,
    dao: KnowledgeDao,
    prefsManager: PreferencesManager
) {
    val context = LocalContext.current
    val allItems by dao.getLeastRecalled().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isFlipped by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    val recentlyShownIds = remember { mutableStateListOf<Long>() }

    // Score preferences
    val familiarBonus by prefsManager.familiarBonus.collectAsState(initial = 1)
    val unfamiliarPenalty by prefsManager.unfamiliarPenalty.collectAsState(initial = 5)

    // Filter out recently shown items
    val items = remember(allItems, recentlyShownIds) {
        allItems.filter { it.id !in recentlyShownIds }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Recall Mode") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val currentItem = items.firstOrNull()

        if (currentItem == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "All caught up!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No items to recall.\nCapture some notes first!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Flashcard
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFlipped) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    onClick = { isFlipped = !isFlipped }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = isFlipped,
                            label = "flip"
                        ) { flipped ->
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (!flipped) {
                                    // Front: Title + tags + recall count
                                    Text(
                                        text = currentItem.title,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    if (currentItem.tags.isNotBlank()) {
                                        Spacer(Modifier.height(16.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            currentItem.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(tag.trim(), fontSize = 12.sp) }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(24.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = "Recalls: ${currentItem.recallCount}",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(Modifier.height(24.dp))
                                    Text(
                                        text = "Tap to reveal",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    // Back: Content + metadata
                                    Text(
                                        text = currentItem.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "${dateFormat.format(Date(currentItem.createdAt))} · ${currentItem.source}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    // Markdown rendering for content
                                    Markdown(
                                        content = currentItem.content,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Unfamiliar button
                    Button(
                        onClick = {
                            VibrationUtil.shortTick(context)
                            scope.launch {
                                val updated = currentItem.copy(recallCount = currentItem.recallCount - unfamiliarPenalty)
                                dao.update(updated)
                                recentlyShownIds.add(currentItem.id)
                                if (recentlyShownIds.size >= 5) recentlyShownIds.clear()
                                isFlipped = false
                                snackbarHostState.showSnackbar("Unfamiliar (-$unfamiliarPenalty) → ${updated.recallCount} recalls")
                            }
                        },
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ThumbDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Unfamiliar", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    // Learned button
                    Button(
                        onClick = {
                            VibrationUtil.shortTick(context)
                            scope.launch {
                                val updated = currentItem.copy(recallCount = currentItem.recallCount + 50)
                                dao.update(updated)
                                recentlyShownIds.add(currentItem.id)
                                if (recentlyShownIds.size >= 5) recentlyShownIds.clear()
                                isFlipped = false
                                snackbarHostState.showSnackbar("Learned (+50) → ${updated.recallCount} recalls")
                            }
                        },
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Learned", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    // Familiar button
                    Button(
                        onClick = {
                            VibrationUtil.shortTick(context)
                            scope.launch {
                                val updated = currentItem.copy(recallCount = currentItem.recallCount + familiarBonus)
                                dao.update(updated)
                                recentlyShownIds.add(currentItem.id)
                                if (recentlyShownIds.size >= 5) recentlyShownIds.clear()
                                isFlipped = false
                                snackbarHostState.showSnackbar("Familiar (+$familiarBonus) → ${updated.recallCount} recalls")
                            }
                        },
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Familiar", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Skip button
                TextButton(
                    onClick = { isFlipped = !isFlipped },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isFlipped) "Show Title" else "Show Content")
                }
            }
        }
    }
}
