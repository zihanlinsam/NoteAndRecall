package com.noteandrecall

import android.app.Application
import com.noteandrecall.data.AppDatabase

class NoteAndRecallApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
