package com.example.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent

class VoiceAssistantAccessibilityService : AccessibilityService() {

    companion object {
        var instance: VoiceAssistantAccessibilityService? = null
            private set

        fun lockDevice(): Boolean {
            val service = instance ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
            return false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
