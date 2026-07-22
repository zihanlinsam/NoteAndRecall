package com.noteandrecall.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.navigation.NavController
import com.mikepenz.markdown.m3.Markdown
import com.noteandrecall.data.KnowledgeDao
import com.noteandrecall.data.KnowledgeItem
import com.noteandrecall.data.PreferencesManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KnowledgeDetailScreen(
    navController: NavController,
    dao: KnowledgeDao,
    prefsManager: PreferencesManager,
    itemId: Long
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
    var item by remember { mutableStateOf<KnowledgeItem?>(null) }
    var isUpdating by remember { mutableStateOf(false) }

    // Score preferences
    val familiarBonus by prefsManager.familiarBonus.collectAsState(initial = 1)
    val unfamiliarPenalty by prefsManager.unfamiliarPenalty.collectAsState(initial = 5)

    // Content edit dialog
    var showContentEditDialog by remember { mutableStateOf(false) }
    var editContent by remember { mutableStateOf("") }

    // Tag edit dialog
    var showTagEditDialog by remember { mutableStateOf(false) }
    var editingTagIndex by remember { mutableIntStateOf(-1) }
    var editTagText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Refresh item
    LaunchedEffect(itemId) {
        item = dao.getById(itemId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Knowledge Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (item != null) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Familiar button
                        Button(
                            onClick = {
                                scope.launch {
                                    isUpdating = true
                                    val current = item ?: return@launch
                                    val updated = current.copy(recallCount = current.recallCount + familiarBonus)
                                    dao.update(updated)
                                    item = updated
                                    isUpdating = false
                                    snackbarHostState.showSnackbar("Marked as familiar (+$familiarBonus recall)")
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = !isUpdating
                        ) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Familiar", fontWeight = FontWeight.Bold)
                        }

                        // Unfamiliar button
                        Button(
                            onClick = {
                                scope.launch {
                                    isUpdating = true
                                    val current = item ?: return@launch
                                    val newCount = current.recallCount - unfamiliarPenalty
                                    val updated = current.copy(recallCount = newCount)
                                    dao.update(updated)
                                    item = updated
                                    isUpdating = false
                                    snackbarHostState.showSnackbar("Marked as unfamiliar (-$unfamiliarPenalty recall)")
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            enabled = !isUpdating
                        ) {
                            Icon(Icons.Default.ThumbDown, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Unfamiliar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        val currentItem = item
        if (currentItem == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = currentItem.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tags - long press to edit
                if (currentItem.tags.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val tagList = currentItem.tags.split(",").filter { it.isNotBlank() }
                        tagList.forEachIndexed { index, tag ->
                            SuggestionChip(
                                onClick = {
                                    editingTagIndex = index
                                    editTagText = tag.trim()
                                    showTagEditDialog = true
                                },
                                label = { Text(tag.trim()) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Metadata card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        MetadataRow("Recalls", "${currentItem.recallCount}")
                        MetadataRow("Created", dateFormat.format(Date(currentItem.createdAt)))
                        MetadataRow("Source", currentItem.source)
                        if (currentItem.location.isNotBlank()) {
                            MetadataRow("Location", currentItem.location)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content divider
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Content",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Content - long press to edit, rendered as Markdown
                Markdown(
                    content = currentItem.content,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    editContent = currentItem.content
                                    showContentEditDialog = true
                                }
                            )
                        }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Delete button
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Content Edit Dialog
    if (showContentEditDialog) {
        AlertDialog(
            onDismissRequest = { showContentEditDialog = false },
            title = { Text("Edit Content") },
            text = {
                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val current = item ?: return@launch
                            val updated = current.copy(content = editContent)
                            dao.update(updated)
                            item = updated
                            showContentEditDialog = false
                            snackbarHostState.showSnackbar("Content updated")
                        }
                    }
                ) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { showContentEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Tag Edit Dialog
    if (showTagEditDialog) {
        AlertDialog(
            onDismissRequest = { showTagEditDialog = false },
            title = { Text("Edit Tag") },
            text = {
                OutlinedTextField(
                    value = editTagText,
                    onValueChange = { editTagText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tag text") },
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val current = item ?: return@launch
                            val tagList = current.tags.split(",").filter { it.isNotBlank() }.toMutableList()
                            if (editingTagIndex in tagList.indices) {
                                tagList[editingTagIndex] = editTagText.trim()
                            }
                            val updated = current.copy(tags = tagList.joinToString(","))
                            dao.update(updated)
                            item = updated
                            showTagEditDialog = false
                            snackbarHostState.showSnackbar("Tag updated")
                        }
                    }
                ) { Text("Submit") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val current = item ?: return@launch
                                val tagList = current.tags.split(",").filter { it.isNotBlank() }.toMutableList()
                                if (editingTagIndex in tagList.indices) {
                                    tagList.removeAt(editingTagIndex)
                                }
                                val updated = current.copy(tags = tagList.joinToString(","))
                                dao.update(updated)
                                item = updated
                                showTagEditDialog = false
                                snackbarHostState.showSnackbar("Tag deleted")
                            }
                        }
                    ) { Text("Delete") }
                    TextButton(onClick = { showTagEditDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to delete this item?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    scope.launch {
                        dao.delete(item!!)
                        navController.popBackStack()
                        Toast.makeText(context, "Delete succeeded", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
