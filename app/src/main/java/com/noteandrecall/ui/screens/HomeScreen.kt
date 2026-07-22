package com.noteandrecall.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.noteandrecall.data.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    prefsManager: PreferencesManager
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isDark by prefsManager.isDarkTheme.collectAsState(initial = false)

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
                        DropdownMenuItem(
                            text = {
                                Text(if (isDark) "Switch to Light Theme" else "Switch to Dark Theme")
                            },
                            onClick = {
                                menuExpanded = false
                                scope.launch { prefsManager.setDarkTheme(!isDark) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Knowledge List") },
                            onClick = {
                                menuExpanded = false
                                navController.navigate("knowledge_list")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("AI Config") },
                            onClick = {
                                menuExpanded = false
                                navController.navigate("ai_config")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import / Export") },
                            onClick = {
                                menuExpanded = false
                                navController.navigate("import_export")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Score Settings") },
                            onClick = {
                                menuExpanded = false
                                navController.navigate("score_settings")
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("📖 Help") },
                            onClick = {
                                menuExpanded = false
                                navController.navigate("help")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📋 Log") },
                            onClick = {
                                menuExpanded = false
                                navController.navigate("log")
                            }
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
            Button(
                onClick = { navController.navigate("note") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📝",
                        fontSize = 40.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Note",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Capture knowledge instantly",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate("recall") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🧠",
                        fontSize = 40.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recall",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Review what you've learned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
