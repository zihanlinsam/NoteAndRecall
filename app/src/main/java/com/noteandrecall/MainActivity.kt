package com.noteandrecall

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import androidx.navigation.NavController
import com.noteandrecall.data.AppDatabase
import com.noteandrecall.data.PreferencesManager
import com.noteandrecall.ui.navigation.MainNavGraph
import com.noteandrecall.ui.theme.NoteAndRecallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dao = AppDatabase.getDatabase(this).knowledgeDao()
        val prefsManager = PreferencesManager(this)

        setContent {
            NoteAndRecallTheme(prefsManager = prefsManager) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PermissionsGate {
                        MainNavGraph(dao = dao, prefsManager = prefsManager)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionsGate(content: @Composable () -> Unit) {
    val context = LocalContext.current

    // Permissions to request (varies by API level)
    val permissionsToRequest = remember {
        mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    // Check which permissions are already granted
    val allGranted = permissionsToRequest.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var currentPermissionIndex by remember { mutableIntStateOf(-1) }
    var hasStarted by remember { mutableStateOf(false) }

    // Request each un-granted permission sequentially
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* move to next */ }

    LaunchedEffect(Unit) {
        if (!allGranted) {
            for (i in permissionsToRequest.indices) {
                if (ContextCompat.checkSelfPermission(context, permissionsToRequest[i]) != PackageManager.PERMISSION_GRANTED) {
                    currentPermissionIndex = i
                    launcher.launch(permissionsToRequest[i])
                    break
                }
            }
        }
        hasStarted = true
    }

    // After each request, check if more are needed
    LaunchedEffect(currentPermissionIndex) {
        if (currentPermissionIndex >= 0 && hasStarted) {
            for (i in (currentPermissionIndex + 1) until permissionsToRequest.size) {
                if (ContextCompat.checkSelfPermission(context, permissionsToRequest[i]) != PackageManager.PERMISSION_GRANTED) {
                    currentPermissionIndex = i
                    launcher.launch(permissionsToRequest[i])
                    return@LaunchedEffect
                }
            }
            // All done
        }
    }

    if (allGranted) {
        content()
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Text("Note & Recall", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text("This app needs the following permissions:", textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("🎤 Microphone (voice notes)", fontSize = 14.sp)
                Text("📷 Camera (image capture)", fontSize = 14.sp)
                Text("📍 Location (auto-tagging)", fontSize = 14.sp)
                Text("🖼️ Media (gallery access)", fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                if (currentPermissionIndex >= 0) {
                    Text("Granting permission ${currentPermissionIndex + 1} of ${permissionsToRequest.size}...",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
