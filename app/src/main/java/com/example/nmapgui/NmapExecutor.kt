package com.example.nmapgui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class NmapExecutor {

    companion object {
        /** Common nmap binary paths on rooted Android. */
        val CANDIDATE_PATHS = listOf(
            "/data/data/com.termux/files/usr/bin/nmap",
            "/data/adb/magisk/usr/bin/nmap",
            "/system/xbin/nmap",
            "/system/bin/nmap",
            "/usr/bin/nmap",
            "/usr/local/bin/nmap",
        )

        fun findNmap(customPath: String = ""): String? {
            if (customPath.isNotBlank() && File(customPath).canExecute()) return customPath
            return CANDIDATE_PATHS.firstOrNull { File(it).canExecute() }
        }
    }

    private var currentProcess: Process? = null

    /**
     * Run nmap with the given argument list, streaming each output line
     * to [outputChannel]. Returns exit code.
     */
    suspend fun runScan(
        command: List<String>,
        outputChannel: SendChannel<String>
    ): Int = withContext(Dispatchers.IO) {
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        currentProcess = process

        try {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line ?: break
                    if (!outputChannel.isClosedForSend) {
                        outputChannel.trySend(l)
                    }
                }
            }
            process.waitFor()
        } finally {
            currentProcess = null
        }
    }

    fun stopScan() {
        currentProcess?.destroy()
        currentProcess = null
    }
}
