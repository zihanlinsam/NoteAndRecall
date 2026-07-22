package com.noteandrecall.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_items")
data class KnowledgeItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val tags: String = "",
    val recallCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val location: String = "",
    val source: String = "TEXT"
)
