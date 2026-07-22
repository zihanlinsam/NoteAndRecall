package com.noteandrecall.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.noteandrecall.data.KnowledgeDao
import com.noteandrecall.data.KnowledgeItem
import com.noteandrecall.data.PreferencesManager
import com.noteandrecall.util.AiClient
import com.noteandrecall.util.AudioRecorder
import com.noteandrecall.util.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

enum class NoteMode { AUTO, AUTO_EDIT, MANUAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    navController: NavController,
    dao: KnowledgeDao,
    prefsManager: PreferencesManager,
    mode: NoteMode = NoteMode.MANUAL,
    initialTitle: String = "",
    initialTags: String = "",
    initialContent: String = ""
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var content by remember { mutableStateOf(initialContent) }
    var title by remember { mutableStateOf(initialTitle) }
    var tags by remember { mutableStateOf(initialTags) }
    var isPolished by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }
    var recordingDurationMs by remember { mutableStateOf(0L) }
    var recordStartTime by remember { mutableStateOf(0L) }
    var existingTags by remember { mutableStateOf<List<String>>(emptyList()) }
    // Auto mode state
    var questionText by remember { mutableStateOf("") }
    var isAskingAi by remember { mutableStateOf(false) }

    // Load existing tags
    LaunchedEffect(Unit) {
        try {
            val raw = dao.getAllTagsStrings()
            existingTags = raw.flatMap { it.split(",") }.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        } catch (_: Exception) { }
    }

    val aiEndpoint by prefsManager.aiEndpoint.collectAsState(initial = "")
    val aiApiKey by prefsManager.aiApiKey.collectAsState(initial = "")
    val aiModel by prefsManager.aiModel.collectAsState(initial = "")

    var currentLocation by remember { mutableStateOf("") }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Ask AI: generate card from text question
    suspend fun askAiFromQuestion(q: String) {
        val prompt = """Generate a well-structured knowledge note from this question/topic.

Rules:
- Generate a concise title (max 8 words)
- Content should be informative, at least 150 words, and well-structured in Markdown
- Add relevant tags in #format (max 3, prefer existing ones)
- Match the language of the question below (if Chinese, output Chinese; if English, output English)
- Return EXACTLY this format:
TITLE: [title]
TAGS: [#tag1 #tag2]
CONTENT: [content]

Topic: $q"""
        val result = AiClient.chat(aiEndpoint, aiApiKey, aiModel, prompt)
        val lines = result.lines()
        var contentLines = mutableListOf<String>()
        for (line in lines) {
            when {
                line.startsWith("TITLE:") -> title = line.removePrefix("TITLE:").trim()
                line.startsWith("TAGS:") -> tags = line.removePrefix("TAGS:").trim()
                    .replace("#", "").replace("[", "").replace("]", "").replace(" ", ",")
                line.startsWith("CONTENT:") -> contentLines.add(line.removePrefix("CONTENT:").trim())
                contentLines.isNotEmpty() -> contentLines.add(line)
            }
        }
        if (contentLines.isNotEmpty()) content = contentLines.joinToString("\n")
    }

    // After AI generation, navigate to AUTO_EDIT with filled fields
    fun navigateToEdit() {
        navController.navigate("note_edit?title=${java.net.URLEncoder.encode(title, "UTF-8")}&tags=${java.net.URLEncoder.encode(tags, "UTF-8")}&content=${java.net.URLEncoder.encode(content, "UTF-8")}") {
            popUpTo("home")
        }
    }

    fun compressImageBytes(bytes: ByteArray): ByteArray {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        val origW = opts.outWidth; val origH = opts.outHeight
        val maxDim = 1024; var scale = 1
        while (origW / scale > maxDim || origH / scale > maxDim) scale *= 2
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = scale }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return bytes
        val out = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out); bitmap.recycle()
        return out.toByteArray()
    }

    suspend fun processImageBytes(bytes: ByteArray, mimeType: String) {
        val compressed = compressImageBytes(bytes)
        val base64 = Base64.getEncoder().encodeToString(compressed)
        val result = AiClient.extractFromImage(aiEndpoint, aiApiKey, aiModel, base64, mimeType, existingTags)
        val lines = result.lines()
        var imgTitle = ""; var imgTags = ""; val contentLines = mutableListOf<String>()
        for (line in lines) {
            when {
                line.startsWith("TITLE:") -> imgTitle = line.removePrefix("TITLE:").trim()
                line.startsWith("TAGS:") -> imgTags = line.removePrefix("TAGS:").trim()
                line.startsWith("CONTENT:") -> contentLines.add(line.removePrefix("CONTENT:").trim())
                contentLines.isNotEmpty() -> contentLines.add(line)
            }
        }
        if (title.isBlank()) title = imgTitle
        if (tags.isBlank()) tags = imgTags.replace("#", "").replace("[", "").replace("]", "").replace(" ", ",")
        content = contentLines.joinToString("\n")
    }

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) scope.launch {
            isProcessing = true
            try {
                val bytes = context.contentResolver.openInputStream(photoUri!!)?.readBytes()
                if (bytes != null) { processImageBytes(bytes, "image/jpeg"); if (mode == NoteMode.AUTO) navigateToEdit() }
            } catch (e: Exception) { snackbarHostState.showSnackbar("Failed: ${e.message}") }
            finally { isProcessing = false }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            isProcessing = true
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                if (bytes != null) { processImageBytes(bytes, context.contentResolver.getType(uri) ?: "image/jpeg"); if (mode == NoteMode.AUTO) navigateToEdit() }
            } catch (e: Exception) { snackbarHostState.showSnackbar("Failed: ${e.message}") }
            finally { isProcessing = false }
        }
    }

    var showCaptureDialog by remember { mutableStateOf(false) }

    fun fetchLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? -> loc?.let { currentLocation = "${it.latitude},${it.longitude}" } }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> if (isGranted) fetchLocation() }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) fetchLocation()
        else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Top bar title based on mode
    val topBarTitle = when (mode) {
        NoteMode.AUTO -> "Auto Note"
        NoteMode.AUTO_EDIT -> "Edit Note"
        NoteMode.MANUAL -> "Manual Note"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle) },
                navigationIcon = {
                    IconButton(onClick = { if (navController.previousBackStackEntry != null) navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            // ===== Unified Loading Indicator =====
            val loadingText = when {
                isAskingAi -> "🤖 Asking AI..."
                isRecording -> "🎤 Recording..."
                isTranscribing -> "⏳ Transcribing..."
                isProcessing -> "⏳ Processing..."
                else -> null
            }
            if (loadingText != null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(loadingText, style = MaterialTheme.typography.bodySmall,
                    color = when { isRecording -> MaterialTheme.colorScheme.error else -> MaterialTheme.colorScheme.primary })
                Spacer(Modifier.height(8.dp))
            }

            // ===== AUTO MODE: Question + Ask AI + Speak + Capture =====
            if (mode == NoteMode.AUTO) {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("Question") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))

                // Ask AI button
                Button(
                    onClick = {
                        if (questionText.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Enter a question first") }
                            return@Button
                        }
                        scope.launch {
                            isAskingAi = true
                            try { askAiFromQuestion(questionText); navigateToEdit() }
                            catch (e: Exception) { snackbarHostState.showSnackbar("AI failed: ${e.message}") }
                            finally { isAskingAi = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = questionText.isNotBlank() && !isAskingAi && !isProcessing && !isRecording,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isAskingAi) "Thinking..." else "Ask AI", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))
                Text("or", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                // Speak + Capture buttons in Auto mode
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Speak
                    OutlinedButton(
                        onClick = {
                            LogManager.i("NoteScreen", "Auto Speek: clicked")
                            if (isRecording) {
                                AudioRecorder.stop()
                                isRecording = false
                                // Processing happens in the recording coroutine below
                                return@OutlinedButton
                            }
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); return@OutlinedButton
                            }
                            isRecording = true
                            File(context.cacheDir, "audio_recordings").mkdirs()
                            scope.launch {
                                try {
                                    val result = AudioRecorder.record(File(context.cacheDir, "audio_recordings"))
                                    val audioFile = File(result.filePath)
                                    if (!audioFile.exists() || audioFile.length() == 0L || result.durationMs < 500) {
                                        audioFile.delete(); return@launch
                                    }
                                    isTranscribing = true
                                    val audioBytes = withContext(Dispatchers.IO) { audioFile.readBytes() }
                                    val transcribed = AiClient.transcribeAudio(aiEndpoint, aiApiKey, aiModel, audioBytes, "recording.wav")
                                    content = transcribed; isPolished = true
                                    val polishResult = AiClient.polishNote(aiEndpoint, aiApiKey, aiModel, content, existingTags)
                                    val newCl = mutableListOf<String>()
                                    for (l in polishResult.lines()) {
                                        when { l.startsWith("TITLE:") -> title = l.removePrefix("TITLE:").trim()
                                            l.startsWith("TAGS:") -> tags = l.removePrefix("TAGS:").trim().replace("#", "").replace("[", "").replace("]", "").replace(" ", ",")
                                            l.startsWith("CONTENT:") -> newCl.add(l.removePrefix("CONTENT:").trim())
                                            newCl.isNotEmpty() -> newCl.add(l) }
                                    }
                                    if (newCl.isNotEmpty()) content = newCl.joinToString("\n")
                                    navigateToEdit()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Failed: ${e.message}")
                                } finally {
                                    isRecording = false; isTranscribing = false
                                    try { File(context.cacheDir, "audio_recordings/recording.wav").delete() } catch (_: Exception) { }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (isRecording) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.outlinedButtonColors(),
                        enabled = !isAskingAi && !isProcessing && !isTranscribing
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(24.dp))
                            Text(if (isRecording) "Recording..." else if (isTranscribing) "Transcribing..." else "Speak", fontSize = 11.sp)
                        }
                    }

                    // Capture
                    OutlinedButton(
                        onClick = { showCaptureDialog = true },
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isAskingAi && !isProcessing && !isRecording
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(24.dp))
                            Text("Capture", fontSize = 11.sp)
                        }
                    }
                }
            }

            // ===== MANUAL & AUTO_EDIT: Title + Tags + Content + Polish + Submit =====
            if (mode == NoteMode.MANUAL || mode == NoteMode.AUTO_EDIT) {
                // Title
                OutlinedTextField(value = title, onValueChange = { title = it; isPolished = false }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))

                // Tags
                OutlinedTextField(value = tags, onValueChange = { tags = it; isPolished = false }, label = { Text("Tags (comma separated)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))

                // Content
                OutlinedTextField(value = content, onValueChange = { content = it; isPolished = false }, label = { Text("Note content (required)") }, modifier = Modifier.fillMaxWidth().weight(0.5f), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(12.dp))

                // Polish button only (no Speak/Capture in manual/edit)
                if (mode == NoteMode.MANUAL) {
                    OutlinedButton(onClick = {
                        if (content.isBlank()) { scope.launch { snackbarHostState.showSnackbar("Enter some content first") }; return@OutlinedButton }
                        scope.launch {
                            isProcessing = true
                            try {
                                val result = AiClient.polishNote(aiEndpoint, aiApiKey, aiModel, content, existingTags)
                                val pCl = mutableListOf<String>()
                                for (line in result.lines()) {
                                    when { line.startsWith("TITLE:") -> title = line.removePrefix("TITLE:").trim()
                                        line.startsWith("TAGS:") -> tags = line.removePrefix("TAGS:").trim().replace("#", "").replace("[", "").replace("]", "").replace(" ", ",")
                                        line.startsWith("CONTENT:") -> pCl.add(line.removePrefix("CONTENT:").trim())
                                        pCl.isNotEmpty() -> pCl.add(line) }
                                }
                                if (pCl.isNotEmpty()) content = pCl.joinToString("\n")
                                isPolished = true; snackbarHostState.showSnackbar("Note polished!")
                            } catch (e: Exception) { snackbarHostState.showSnackbar("Polishing failed: ${e.message}") }
                            finally { isProcessing = false }
                        }
                    }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), enabled = !isProcessing && !isRecording && !isTranscribing) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp))
                        Text(if (isProcessing) "..." else "Polish")
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Submit
                Button(
                    onClick = {
                        if (content.isBlank()) { scope.launch { snackbarHostState.showSnackbar("Content cannot be empty") }; return@Button }
                        scope.launch {
                            try {
                                var finalTitle = title; var finalTags = tags
                                if (finalTitle.isBlank()) { finalTitle = content.trim().split("\\s+".toRegex()).take(3).joinToString(" "); if (finalTitle.length > 50) finalTitle = finalTitle.take(47) + "..." }
                                if (finalTags.isBlank()) finalTags = "Others"
                                dao.insert(KnowledgeItem(title = finalTitle, content = content, tags = finalTags, recallCount = 0, location = currentLocation, source = "TEXT"))
                                content = ""; title = ""; tags = ""; isPolished = false
                                navController.popBackStack(); Toast.makeText(context, "Submit succeeded", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) { snackbarHostState.showSnackbar("Save failed: ${e.message}") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp),
                    enabled = content.isNotBlank() && !isProcessing && !isRecording && !isTranscribing
                ) { Text("Submit", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }

    // Capture dialog
    if (showCaptureDialog) {
        AlertDialog(
            onDismissRequest = { showCaptureDialog = false },
            title = { Text("Capture Knowledge") }, text = { Text("Choose a source:") },
            confirmButton = { TextButton(onClick = { showCaptureDialog = false; val f = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg"); photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f); cameraLauncher.launch(photoUri!!) }) { Text("Camera") } },
            dismissButton = { TextButton(onClick = { showCaptureDialog = false; galleryLauncher.launch("image/*") }) { Text("Gallery") } }
        )
    }
}
