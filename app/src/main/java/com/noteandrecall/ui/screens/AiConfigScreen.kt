package com.noteandrecall.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.noteandrecall.data.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConfigScreen(
    navController: NavController,
    prefsManager: PreferencesManager
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentEndpoint by prefsManager.aiEndpoint.collectAsState(initial = "")
    val currentApiKey by prefsManager.aiApiKey.collectAsState(initial = "")
    val currentModel by prefsManager.aiModel.collectAsState(initial = "")

    var endpoint by remember(currentEndpoint) { mutableStateOf(currentEndpoint) }
    var apiKey by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var model by remember(currentModel) { mutableStateOf(currentModel) }
    var isSaved by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AI Configuration") },
                navigationIcon = {
                    IconButton(onClick = { if (navController.previousBackStackEntry != null) navController.popBackStack() }) {
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
                .padding(16.dp)
        ) {
            Text(
                text = "Configure your AI provider for note polishing and image extraction.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it; isSaved = false },
                label = { Text("API Endpoint") },
                placeholder = { Text("https://api.deepseek.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it; isSaved = false },
                label = { Text("API Key") },
                placeholder = { Text("sk-...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = model,
                onValueChange = { model = it; isSaved = false },
                label = { Text("Model Name") },
                placeholder = { Text("deepseek-chat") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        prefsManager.setAiEndpoint(endpoint.trim())
                        prefsManager.setAiApiKey(apiKey.trim())
                        prefsManager.setAiModel(model.trim())
                        isSaved = true
                        snackbarHostState.showSnackbar("Settings saved!")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isSaved) "Saved!" else "Save Settings",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test connection button
            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            val result = com.noteandrecall.util.AiClient.chat(
                                endpoint = endpoint.trim(),
                                apiKey = apiKey.trim(),
                                model = model.trim(),
                                prompt = "Reply with just: OK"
                            )
                            snackbarHostState.showSnackbar("Connection successful: $result")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Connection failed: ${e.message}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Test Connection")
            }
        }
    }
}
