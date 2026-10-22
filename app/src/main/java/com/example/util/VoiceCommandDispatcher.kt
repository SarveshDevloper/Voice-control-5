package com.example.util

sealed interface VoiceCommandResult {
    data object LockScreen : VoiceCommandResult
    data object UnlockScreen : VoiceCommandResult
    data class LaunchApp(val appName: String) : VoiceCommandResult
    data class Unknown(val rawText: String) : VoiceCommandResult
}

object VoiceCommandDispatcher {

    fun parse(spokenText: String): VoiceCommandResult {
        val text = spokenText.trim().lowercase()

        // Lock commands
        if (text.contains("screen lock") || text.contains("lock screen") ||
            text.contains("phone lock") || text.contains("lock karo") || text == "lock"
        ) {
            return VoiceCommandResult.LockScreen
        }

        // Unlock commands
        if (text.contains("screen unlock") || text.contains("unlock screen") ||
            text.contains("phone unlock") || text.contains("unlock karo") || text == "unlock"
        ) {
            return VoiceCommandResult.UnlockScreen
        }

        // App launch commands
        val openPrefixes = listOf("open ", "kholo ", "launch ", "start ", "chalao ")
        for (prefix in openPrefixes) {
            if (text.startsWith(prefix)) {
                val appName = text.removePrefix(prefix).trim()
                if (appName.isNotEmpty()) {
                    return VoiceCommandResult.LaunchApp(appName)
                }
            }
        }

        return VoiceCommandResult.Unknown(spokenText)
    }
}
