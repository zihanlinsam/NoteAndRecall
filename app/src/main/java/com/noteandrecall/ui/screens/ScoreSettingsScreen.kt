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
fun ScoreSettingsScreen(
    navController: NavController,
    prefsManager: PreferencesManager
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentFamiliarBonus by prefsManager.familiarBonus.collectAsState(initial = 1)
    val currentUnfamiliarPenalty by prefsManager.unfamiliarPenalty.collectAsState(initial = 5)

    var familiarBonus by remember(currentFamiliarBonus) { mutableStateOf(currentFamiliarBonus.toString()) }
    var unfamiliarPenalty by remember(currentUnfamiliarPenalty) { mutableStateOf(currentUnfamiliarPenalty.toString()) }
    var isSaved by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Score Settings") },
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
                .padding(16.dp)
        ) {
            Text(
                text = "Customize the score changes when reviewing items.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = familiarBonus,
                onValueChange = {
                    familiarBonus = it.filter { c -> c.isDigit() || c == '-' }
                    isSaved = false
                },
                label = { Text("Familiar Bonus") },
                placeholder = { Text("1") },
                supportingText = { Text("Points added when marked Familiar") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = unfamiliarPenalty,
                onValueChange = {
                    unfamiliarPenalty = it.filter { c -> c.isDigit() || c == '-' }
                    isSaved = false
                },
                label = { Text("Unfamiliar Penalty") },
                placeholder = { Text("5") },
                supportingText = { Text("Points subtracted when marked Unfamiliar") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        val bonus = familiarBonus.toIntOrNull() ?: 1
                        val penalty = unfamiliarPenalty.toIntOrNull() ?: 5
                        prefsManager.setFamiliarBonus(bonus)
                        prefsManager.setUnfamiliarPenalty(penalty)
                        isSaved = true
                        snackbarHostState.showSnackbar("Score settings saved!")
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
        }
    }
}
