package com.noteandrecall.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Simple in-memory ring-buffer logger.
 * Stores last [MAX_ENTRIES] entries so the user can inspect runtime behavior.
 */
object LogManager {
    private const val MAX_ENTRIES = 200
    private val entries = mutableListOf<LogEntry>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    data class LogEntry(
        val timestamp: Long,
        val level: String,
        val tag: String,
        val message: String
    ) {
        fun formatted(): String {
            val ts = dateFormat.format(Date(timestamp))
            return "[$ts][$level][$tag] $message"
        }
    }

    @Synchronized
    fun i(tag: String, message: String) = add("I", tag, message)

    @Synchronized
    fun w(tag: String, message: String) = add("W", tag, message)

    @Synchronized
    fun e(tag: String, message: String) = add("E", tag, message)

    @Synchronized
    private fun add(level: String, tag: String, message: String) {
        entries.add(LogEntry(System.currentTimeMillis(), level, tag, message))
        if (entries.size > MAX_ENTRIES) {
            entries.removeAt(0)
        }
    }

    @Synchronized
    fun getAll(): List<LogEntry> = entries.toList()

    @Synchronized
    fun getText(): String = entries.joinToString("\n") { it.formatted() }

    @Synchronized
    fun clear() = entries.clear()
}
