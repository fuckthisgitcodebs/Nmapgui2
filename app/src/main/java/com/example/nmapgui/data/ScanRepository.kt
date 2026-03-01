package com.example.nmapgui.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

class ScanRepository(private val context: Context) {

    private val historyFile: File
        get() = File(context.filesDir, "scan_history.json")

    fun loadHistory(): MutableList<ScanHistoryEntry> {
        return try {
            if (historyFile.exists()) {
                json.decodeFromString(historyFile.readText())
            } else {
                mutableListOf()
            }
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun saveHistory(history: List<ScanHistoryEntry>) {
        try {
            historyFile.writeText(json.encodeToString(history))
        } catch (_: Exception) {}
    }

    fun addEntry(entry: ScanHistoryEntry) {
        val history = loadHistory()
        history.add(0, entry)
        // Keep last 50 scans
        val trimmed = history.take(50)
        saveHistory(trimmed)
    }

    fun deleteEntry(id: String) {
        val history = loadHistory().filter { it.id != id }
        saveHistory(history)
    }

    fun clearHistory() = saveHistory(emptyList())
}
