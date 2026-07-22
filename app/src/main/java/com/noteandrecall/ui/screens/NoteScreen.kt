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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    navController: NavController,
    dao: KnowledgeDao,
    prefsManager: PreferencesManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form state
    var content by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var isPolished by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }
    var isExtending by remember { mutableStateOf(false) }
    var recordingDurationMs by remember { mutableStateOf(0L) }
    var recordStartTime by remember { mutableStateOf(0L) }
    var existingTags by remember { mutableStateOf<List<String>>(emptyList()) }

    // Load existing tags from DB
    LaunchedEffect(Unit) {
        try {
            val raw = dao.getAllTagsStrings()
            existingTags = raw.flatMap { it.split(",") }.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        } catch (_: Exception) { }
    }

    // AI config
    val aiEndpoint by prefsManager.aiEndpoint.collectAsState(initial = "")
    val aiApiKey by prefsManager.aiApiKey.collectAsState(initial = "")
    val aiModel by prefsManager.aiModel.collectAsState(initial = "")

    // Location
    var currentLocation by remember { mutableStateOf("") }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    /** Compress image bytes to max 1920px @ 80% quality to control API payload size */
    fun compressImageBytes(bytes: ByteArray): ByteArray {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        val origW = opts.outWidth; val origH = opts.outHeight
        LogManager.i("NoteScreen", "compressImage: original ${origW}x${origH} size=${bytes.size}")
        val maxDim = 1024
        var scale = 1
        while (origW / scale > maxDim || origH / scale > maxDim) scale *= 2
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = scale }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            ?: return bytes
        val out = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
        bitmap.recycle()
        LogManager.i("NoteScreen", "compressImage: compressed -> ${out.size()} bytes")
        return out.toByteArray()
    }

    /** Shared image handler: compress → base64 → API call → parse into form fields */
    suspend fun processImageBytes(bytes: ByteArray, mimeType: String) {
        LogManager.i("NoteScreen", "processImageBytes: input=${bytes.size} mime=$mimeType")
        val compressed = compressImageBytes(bytes)
        val base64 = Base64.getEncoder().encodeToString(compressed)
        LogManager.i("NoteScreen", "processImageBytes: base64.length=${base64.length}")
        try {
            val result = AiClient.extractFromImage(aiEndpoint, aiApiKey, aiModel, base64, mimeType, existingTags)
            LogManager.i("NoteScreen", "processImageBytes: API OK, parsing result")
            val lines = result.lines()
            var imgTitle = ""
            var imgTags = ""
            val contentLines = mutableListOf<String>()
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
            LogManager.i("NoteScreen", "processImageBytes: parsed OK title='$imgTitle' tags='$imgTags'")
        } catch (e: Exception) {
            LogManager.e("NoteScreen", "processImageBytes: FAILED: ${e.message}")
            throw e
        }
    }

    // Capture photo URI
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // Camera launcher (TakePicture saves to file via FileProvider)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            scope.launch {
                if (aiEndpoint.isNotBlank() && aiApiKey.isNotBlank()) {
                    isProcessing = true
                    try {
                        val inputStream = context.contentResolver.openInputStream(photoUri!!)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()
                        if (bytes != null) {
                            processImageBytes(bytes, "image/jpeg")
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Image analysis failed: ${e.message}")
                    } finally {
                        isProcessing = false
                    }
                } else {
                    snackbarHostState.showSnackbar("Configure AI settings first to extract from images")
                }
            }
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                if (aiEndpoint.isNotBlank() && aiApiKey.isNotBlank()) {
                    isProcessing = true
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()
                        if (bytes != null) {
                            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                            processImageBytes(bytes, mimeType)
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Image analysis failed: ${e.message}")
                    } finally {
                        isProcessing = false
                    }
                } else {
                    snackbarHostState.showSnackbar("Configure AI settings first to extract from images")
                }
            }
        }
    }

    // Capture dialog state
    var showCaptureDialog by remember { mutableStateOf(false) }

    // Get location
    fun fetchLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                loc?.let {
                    currentLocation = "${it.latitude},${it.longitude}"
                }
            }
        }
    }

    // Request permissions on launch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) fetchLocation()
    }

    // Fetch location on composition
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocation()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("New Note") },
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
            // Title field (always visible)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Tags hint (always visible)
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Main input - 50% height
            OutlinedTextField(
                value = content,
                onValueChange = {
                    content = it
                    isPolished = false
                },
                label = { Text("Note content (required)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Recording/Transcribing indicator
            if (isRecording) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "🎤 Recording...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(4.dp))
            }
            if (isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "⏳ Processing...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
            }
            if (isTranscribing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "⏳ Transcribing...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
            }

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Speak button — tap to start recording, tap again to stop & transcribe
                OutlinedButton(
                    onClick = {
                        LogManager.i("NoteScreen", "Speak: clicked, isRecording=$isRecording")
                        if (isRecording) {
                            // STOP
                            LogManager.i("NoteScreen", "Speak: stopping recording...")
                            AudioRecorder.stop()
                            recordingDurationMs = System.currentTimeMillis() - recordStartTime
                            isRecording = false
                            scope.launch {
                                val audioFile = File(context.cacheDir, "audio_recordings/recording.wav")
                                if (!audioFile.exists() || audioFile.length() == 0L) {
                                    snackbarHostState.showSnackbar("No audio recorded")
                                    return@launch
                                }
                                if (recordingDurationMs < 500) {
                                    audioFile.delete()
                                    snackbarHostState.showSnackbar("Recording too short (${recordingDurationMs}ms)")
                                    return@launch
                                }
                                isTranscribing = true
                                try {
                                    val audioBytes = withContext(Dispatchers.IO) { audioFile.readBytes() }
                                    LogManager.i("NoteScreen", "Speak: read ${audioBytes.size}B, transcribing...")
                                    val transcribed = AiClient.transcribeAudio(
                                        aiEndpoint, aiApiKey, aiModel, audioBytes, "recording.wav"
                                    )
                                    content = if (content.isBlank()) transcribed else "$content\n$transcribed"
                                    val polishResult = AiClient.polishNote(aiEndpoint, aiApiKey, aiModel, content, existingTags)
                                    val polishLines = polishResult.lines()
                                    val newContentLines = mutableListOf<String>()
                                    for (line in polishLines) {
                                        when {
                                            line.startsWith("TITLE:") -> title = line.removePrefix("TITLE:").trim()
                                            line.startsWith("TAGS:") -> tags = line.removePrefix("TAGS:").trim()
                                                .replace("#", "").replace("[", "").replace("]", "")
                                                .replace(" ", ",")
                                            line.startsWith("CONTENT:") -> newContentLines.add(line.removePrefix("CONTENT:").trim())
                                            newContentLines.isNotEmpty() -> newContentLines.add(line)
                                        }
                                    }
                                    if (newContentLines.isNotEmpty()) {
                                        content = newContentLines.joinToString("\n")
                                    }
                                    isPolished = true
                                    snackbarHostState.showSnackbar("Record complete ${recordingDurationMs / 1000}s")
                                } catch (e: Exception) {
                                    LogManager.e("NoteScreen", "Speak: error: ${e.message}")
                                    snackbarHostState.showSnackbar("Failed: ${e.message}")
                                } finally {
                                    isTranscribing = false
                                    audioFile.delete()
                                }
                            }
                        } else {
                            // START
                            LogManager.i("NoteScreen", "Speak: start clicked")
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@OutlinedButton
                            }
                            if (aiEndpoint.isBlank() || aiApiKey.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("Configure AI settings first") }
                                return@OutlinedButton
                            }
                            isRecording = true
                            File(context.cacheDir, "audio_recordings").mkdirs()
                            scope.launch {
                                try { AudioRecorder.record(File(context.cacheDir, "audio_recordings")) }
                                catch (e: Exception) { LogManager.e("NoteScreen", "Speak: record() error: ${e.message}"); isRecording = false }
                            }
                        }
                    },
                    colors = if (isRecording) ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.error
                    ) else ButtonDefaults.outlinedButtonColors(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    // Keep enabled during recording so user can tap to stop
                    enabled = !isProcessing && !isTranscribing,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(if (isRecording) "Recording..." else if (isTranscribing) "Transcribing..." else "Speak",
                            fontSize = 11.sp, maxLines = 1)
                    }
                }

                // Capture button
                OutlinedButton(
                    onClick = { showCaptureDialog = true },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    enabled = !isProcessing && !isRecording && !isTranscribing,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("Capture", fontSize = 11.sp, maxLines = 1)
                    }
                }

                // AI Polish button — completes Title, Tags, and Content
                OutlinedButton(onClick = {
                        if (content.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Enter some content first") }
                            return@OutlinedButton
                        }
                        if (aiEndpoint.isBlank() || aiApiKey.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Configure AI settings first")
                            }
                            return@OutlinedButton
                        }
                        scope.launch {
                            isProcessing = true
                            try {
                                val result = AiClient.polishNote(aiEndpoint, aiApiKey, aiModel, content, existingTags)
                                val lines = result.lines()
                                for (line in lines) {
                                    when {
                                        line.startsWith("TITLE:") -> title = line.removePrefix("TITLE:").trim()
                                        line.startsWith("TAGS:") -> {
                                            tags = line.removePrefix("TAGS:").trim()
                                                .replace("#", "").replace("[", "").replace("]", "")
                                                .replace(" ", ",")
                                        }
                                        line.startsWith("CONTENT:") -> {
                                            content = line.removePrefix("CONTENT:").trim()
                                        }
                                    }
                                }
                                isPolished = true
                                snackbarHostState.showSnackbar("Note polished!")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Polishing failed: ${e.message}")
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    enabled = !isProcessing && !isRecording && !isTranscribing,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(if (isProcessing) "..." else "Polish", fontSize = 11.sp, maxLines = 1)
                    }
                }

            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location display
            if (currentLocation.isNotBlank()) {
                Text(
                    text = "📍 $currentLocation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Full-width Submit button
            Button(
                onClick = {
                    if (content.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Content cannot be empty") }
                        return@Button
                    }
                    scope.launch {
                        try {
                            // Simple rules — no AI on submit
                            var finalTitle = title
                            var finalTags = tags
                            if (finalTitle.isBlank()) {
                                finalTitle = content.trim().split("\\s+".toRegex()).take(3).joinToString(" ")
                                if (finalTitle.length > 50) finalTitle = finalTitle.take(47) + "..."
                            }
                            if (finalTags.isBlank()) {
                                finalTags = "Others"
                            }

                            val item = KnowledgeItem(
                                title = finalTitle,
                                content = content,
                                tags = finalTags,
                                recallCount = 0,
                                location = currentLocation,
                                source = "TEXT"
                            )
                            dao.insert(item)
                            content = ""
                            title = ""
                            tags = ""
                            isPolished = false
                            navController.popBackStack()
                            Toast.makeText(context, "Submit succeeded", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Save failed: ${e.message}")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = content.isNotBlank() && !isProcessing && !isRecording && !isTranscribing
            ) {
                Text("Submit", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Capture dialog
    if (showCaptureDialog) {
        AlertDialog(
            onDismissRequest = { showCaptureDialog = false },
            title = { Text("Capture Knowledge") },
            text = { Text("Choose a source:") },
            confirmButton = {
                TextButton(onClick = {
                    showCaptureDialog = false
                    // Create temp file for camera photo
                    val photoFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                    photoUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )
                    cameraLauncher.launch(photoUri!!)
                }) {
                    Text("Camera")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCaptureDialog = false
                    galleryLauncher.launch("image/*")
                }) {
                    Text("Gallery")
                }
            }
        )
    }
}
