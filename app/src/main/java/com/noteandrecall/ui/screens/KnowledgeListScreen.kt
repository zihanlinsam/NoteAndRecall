package com.noteandrecall.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.noteandrecall.data.KnowledgeDao
import com.noteandrecall.data.KnowledgeItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KnowledgeListScreen(
    navController: NavController,
    dao: KnowledgeDao
) {
    val allItems by dao.getAllItems().collectAsState(initial = emptyList())
    var searchTag by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("date") }
    var isSearching by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    val scope = rememberCoroutineScope()

    // Multi-select for batch delete
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Extract unique tags from all items
    val allTags = remember(allItems) {
        allItems.flatMap { it.tags.split(",").map { t -> t.trim() }.filter { it.isNotBlank() } }
            .distinct().sorted()
    }

    // Filter by tag, search title, and sort
    val filteredItems = remember(allItems, searchTag, searchText, sortBy) {
        var filtered = allItems
        if (searchText.isNotBlank()) {
            filtered = filtered.filter { it.title.contains(searchText, ignoreCase = true) }
        }
        if (searchTag.isNotBlank()) {
            filtered = filtered.filter { it.tags.contains(searchTag, ignoreCase = true) }
        }

        when (sortBy) {
            "recalls" -> filtered.sortedBy { it.recallCount }
            "alpha" -> filtered.sortedBy { it.title.lowercase() }
            else -> filtered.sortedByDescending { it.createdAt } // date
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        Text("Search")
                    } else {
                        Text(if (selectedIds.isEmpty()) "Knowledge List" else "${selectedIds.size} selected")
                    }
                },
                navigationIcon = {
                    if (isSearching) {
                        IconButton(onClick = { isSearching = false; searchText = "" }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    } else if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    } else {
                        IconButton(onClick = { if (navController.previousBackStackEntry != null) navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    } else if (!isSearching) {
                        IconButton(onClick = { isSearching = true; searchText = "" }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            if (isSearching) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Search titles") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                Spacer(Modifier.height(8.dp))
            }

            // Tag filter chips + Sort chips (hidden during search)
            if (!isSearching) {
                // Tag filter chips
                if (allTags.isNotEmpty()) {
                    Text(
                        text = "Filter by tag:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = searchTag.isBlank(),
                                onClick = { searchTag = "" },
                                label = { Text("All") },
                                leadingIcon = if (searchTag.isBlank()) {
                                    { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                        items(allTags) { tag ->
                            FilterChip(
                                selected = searchTag == tag,
                                onClick = { searchTag = if (searchTag == tag) "" else tag },
                                label = { Text(tag) }
                            )
                        }
                    }
                }

                // Sort chips
                Text(
                    text = "Sort by:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = sortBy == "date",
                            onClick = { sortBy = "date" },
                            label = { Text("Date") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = sortBy == "recalls",
                            onClick = { sortBy = "recalls" },
                            label = { Text("Recalls") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = sortBy == "alpha",
                            onClick = { sortBy = "alpha" },
                            label = { Text("Alphabetical") }
                        )
                    }
                }
            }

            // Item count
            Text(
                text = "${filteredItems.size} items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            searchText.isNotBlank() -> "No results for: \"$searchText\""
                            searchTag.isNotBlank() -> "No items with tag: #$searchTag"
                            else -> "No items yet"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (selectedIds.isNotEmpty()) {
                                            selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
                                        } else {
                                            navController.navigate("detail/${item.id}")
                                        }
                                    },
                                    onLongClick = {
                                        selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
                                    }
                                ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = if (item.id in selectedIds) CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) else CardDefaults.cardColors()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = dateFormat.format(Date(item.createdAt)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (item.tags.isNotBlank()) {
                                    Text(
                                        text = item.tags.split(",").filter { it.isNotBlank() }
                                            .joinToString(" \u2022 ") { "#$it" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.recallCount} recalls",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = item.source,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Items") },
            text = { Text("Are you sure you want to delete ${selectedIds.size} selected items?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    val idsToDelete = selectedIds.toList()
                    selectedIds = emptySet()
                    // Delete sequentially
                    scope.launch {
                        for (id in idsToDelete) {
                            val item = dao.getById(id)
                            if (item != null) dao.delete(item)
                        }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
