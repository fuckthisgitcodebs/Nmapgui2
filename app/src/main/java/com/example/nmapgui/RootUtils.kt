package com.example.nmapgui

object RootUtils {
    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    /** Execute a shell command as root, return stdout lines. */
    fun execRoot(command: String): List<String> {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = process.inputStream.bufferedReader().readLines()
            process.waitFor()
            output
        } catch (e: Exception) {
            listOf("Error: ${e.message}")
        }
    }
}
