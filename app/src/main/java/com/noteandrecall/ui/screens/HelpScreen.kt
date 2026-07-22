package com.noteandrecall.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help") },
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Title
            Text("Note & Recall", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Captures knowledge and reinforces it through spaced recall.",
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))

            // 1. Creating Notes
            SectionTitle("Creating Notes")

            SubTitle("Text Input")
            Bullet("Tap Note on the home screen.")
            Bullet("Fill in Title, Tags (comma-separated), and Note content.")
            Bullet("Tap Submit to save.")
            IndentText("If Title is empty → first 3 words of content are used.")
            IndentText("If Tags are empty → \"Others\" is used automatically.")
            Spacer(Modifier.height(12.dp))

            SubTitle("Speech Input")
            Bullet("Tap Speak to start recording, tap again to stop.")
            Bullet("Audio is transcribed via the configured AI model.")
            Bullet("Content is automatically polished (Title + Tags + expanded Content).")
            Bullet("Recordings shorter than 500ms are discarded.")
            Spacer(Modifier.height(12.dp))

            SubTitle("Image Capture")
            Bullet("Tap Capture → Camera or Gallery.")
            Bullet("Image is compressed (max 1920px, 80% quality) then sent to the AI.")
            Bullet("Extracted text appears in the form fields.")
            Spacer(Modifier.height(12.dp))

            SubTitle("AI Polish")
            Bullet("Tap Polish to have AI refine your note: generate a title, relevant tags (up to 3), and expand content in Markdown.")
            Bullet("Polish reuses existing tags from your database when relevant.")
            Spacer(Modifier.height(20.dp))

            Divider()
            Spacer(Modifier.height(16.dp))

            // 2. Recalling
            SectionTitle("Recalling")
            Bullet("Tap Recall on the home screen.")
            Bullet("A flashcard shows the title. Tap the card to reveal its content.")
            Bullet("Rate your recall with three buttons:")
            Spacer(Modifier.height(8.dp))

            // Score card (instead of table)
            ScoreRow("Unfamiliar (Thumbs Down)", "Score -5", "Couldn't remember")
            Spacer(Modifier.height(4.dp))
            ScoreRow("Familiar (Thumbs Up)", "Score +1", "Mostly remembered")
            Spacer(Modifier.height(4.dp))
            ScoreRow("Learned (Star)", "Score +50", "Know it cold")
            Spacer(Modifier.height(12.dp))

            Bullet("Items with lower scores appear more frequently.")
            Bullet("Dedup resets when you restart recall.")
            Spacer(Modifier.height(20.dp))

            Divider()
            Spacer(Modifier.height(16.dp))

            // 3. Settings
            SectionTitle("Settings")
            SubTitle("AI Config (menu > AI Config)")
            Bullet("Endpoint: URL of the AI API (default: api.xiaomimimo.com/v1).")
            Bullet("API Key: Your authentication key.")
            Bullet("Model: e.g. mimo-v2.5.")
            Spacer(Modifier.height(12.dp))

            SubTitle("Score Settings (menu > Score Settings)")
            Bullet("Familiar Bonus: points added when tapping Familiar (default +1).")
            Bullet("Unfamiliar Penalty: points deducted when tapping Unfamiliar (default -5).")
            Bullet("Learned always adds +50.")
            Spacer(Modifier.height(12.dp))

            SubTitle("Knowledge List (menu > Knowledge List)")
            Bullet("Browse all saved notes with keyword filter and date/recall sort.")
            Bullet("Tap any item to view details and edit content/tags.")
            Bullet("Tap a tag to edit it; long-press content to edit.")
            Spacer(Modifier.height(12.dp))

            SubTitle("Import / Export (menu > Import / Export)")
            Bullet("Export: downloads all notes as a Markdown file.")
            Bullet("Import: loads notes from a previously exported file.")
            Spacer(Modifier.height(20.dp))

            Divider()
            Spacer(Modifier.height(16.dp))

            // 4. Log
            SectionTitle("Log")
            Bullet("Open Log from the menu to see runtime diagnostic messages.")
            Bullet("Useful for debugging AI calls, recording issues, or permission problems.")
            Bullet("Tap Copy to share logs.")
            Spacer(Modifier.height(20.dp))

            Divider()
            Spacer(Modifier.height(16.dp))

            // 5. Tips
            SectionTitle("Tips")
            Bullet("The app works offline for viewing and recalling notes.")
            Bullet("AI features (transcription, polish, image extraction) require internet.")
            Bullet("Tags are comma-separated: science,biology,dna")
            Bullet("Once submitted, items can be edited from the Knowledge List detail view.")
            Bullet("All data is stored locally on your device.")
            Spacer(Modifier.height(24.dp))

            Divider()
            Spacer(Modifier.height(12.dp))
            Text("Note & Recall v3.8 — Built with Kotlin + Jetpack Compose",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SubTitle(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun Bullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("  •  ", fontSize = 14.sp)
        Text(text, fontSize = 14.sp)
    }
}

@Composable
private fun IndentText(text: String) {
    Text("      $text", fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 1.dp))
}

@Composable
private fun ScoreRow(button: String, score: String, desc: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(button, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(score, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}
