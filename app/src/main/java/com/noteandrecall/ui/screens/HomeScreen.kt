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
                        // Theme selector — single cycling button
                        val modeLabels = listOf("\uD83C\uDF17 Auto", "\u2600\uFE0F Light", "\uD83C\uDF19 Dark")
                        DropdownMenuItem(
                            text = { Text(modeLabels[themeMode]) },
                            onClick = { menuExpanded = false; scope.launch { prefsManager.setThemeMode((themeMode + 1) % 3) } }
                        )
                        DropdownMenuItem(
                            text = { Text("Knowledge List") },
                            onClick = { menuExpanded = false; navController.navigate("knowledge_list") }
                        )
                        DropdownMenuItem(
                            text = { Text("AI Config") },
                            onClick = { menuExpanded = false; navController.navigate("ai_config") }
                        )
                        DropdownMenuItem(
                            text = { Text("Import / Export") },
                            onClick = { menuExpanded = false; navController.navigate("import_export") }
                        )
                        DropdownMenuItem(
                            text = { Text("Score Settings") },
                            onClick = { menuExpanded = false; navController.navigate("score_settings") }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("\uD83D\uDCD6 Help") },
                            onClick = { menuExpanded = false; navController.navigate("help") }
                        )
                        DropdownMenuItem(
                            text = { Text("\u2139\uFE0F About") },
                            onClick = { menuExpanded = false; navController.navigate("about") }
                        )
                        DropdownMenuItem(
                            text = { Text("\uD83D\uDCCB Log") },
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
            Button(
                onClick = { navController.navigate("note") },
                modifier = Modifier.fillMaxWidth().height(140.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDCDD", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Note", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Capture knowledge instantly", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
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
