package com.robotmacro.app.util

object RootShell {
    fun isAvailable(): Boolean = runCatching {
        Runtime.getRuntime().exec(arrayOf("which", "su")).waitFor() == 0
    }.getOrDefault(false)

    fun run(command: String): Result<String> = runCatching {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        val output = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()
        if (process.waitFor() == 0) output else error.ifBlank { "Root command failed" }
    }
}
