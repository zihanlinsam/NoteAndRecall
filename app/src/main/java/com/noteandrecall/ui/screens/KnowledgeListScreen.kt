package com.noteandrecall.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeListScreen(
    navController: NavController,
    dao: KnowledgeDao
) {
    val allItems by dao.getAllItems().collectAsState(initial = emptyList())
    var searchTag by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("date") }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    // Extract unique tags from all items
    val allTags = remember(allItems) {
        allItems.flatMap { it.tags.split(",").map { t -> t.trim() }.filter { it.isNotBlank() } }
            .distinct().sorted()
    }

    // Filter by tag and sort
    val filteredItems = remember(allItems, searchTag, sortBy) {
        val filtered = if (searchTag.isBlank()) allItems
        else allItems.filter { it.tags.contains(searchTag, ignoreCase = true) }

        when (sortBy) {
            "recalls" -> filtered.sortedBy { it.recallCount }
            "alpha" -> filtered.sortedBy { it.title.lowercase() }
            else -> filtered.sortedByDescending { it.createdAt } // date
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Knowledge List") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                        text = if (searchTag.isNotBlank()) "No items with tag: #$searchTag"
                        else "No items yet",
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
                                .clickable { navController.navigate("detail/${item.id}") },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                            .joinToString(" • ") { "#$it" },
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
}
