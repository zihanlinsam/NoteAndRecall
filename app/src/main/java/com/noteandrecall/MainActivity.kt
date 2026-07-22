package com.noteandrecall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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
                    MainNavGraph(dao = dao, prefsManager = prefsManager)
                }
            }
        }
    }
}
