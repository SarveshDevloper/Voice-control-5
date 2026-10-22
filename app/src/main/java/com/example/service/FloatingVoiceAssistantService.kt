package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import com.example.util.AppLauncherHelper
import com.example.util.VoiceCommandDispatcher
import com.example.util.VoiceCommandResult

class FloatingVoiceAssistantService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        setupFloatingWidget()
        initSpeechRecognizer()
    }

    private fun startAsForeground() {
        val channelId = "floating_voice_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Floating Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("Voice Assistant Active")
                .setContentText("Tap floating mic to speak commands")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Voice Assistant Active")
                .setContentText("Tap floating mic to speak commands")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build()
        }

        startForeground(1002, notification)
    }

    private fun setupFloatingWidget() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        val imageView = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xCC00E5FF.toInt())
        }
        floatingView = imageView

        imageView.setOnClickListener {
            startListening()
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        imageView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = Math.abs(event.rawX - initialTouchX)
                    val diffY = Math.abs(event.rawY - initialTouchY)
                    if (diffX < 10 && diffY < 10) {
                        view.performClick()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Toast.makeText(this@FloatingVoiceAssistantService, "Listening...", Toast.LENGTH_SHORT).show()
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        Toast.makeText(this@FloatingVoiceAssistantService, "Could not hear, try again", Toast.LENGTH_SHORT).show()
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull() ?: return
                        handleCommand(spoken)
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun handleCommand(spokenText: String) {
        when (val result = VoiceCommandDispatcher.parse(spokenText)) {
            is VoiceCommandResult.LockScreen -> {
                val locked = VoiceAssistantAccessibilityService.lockDevice()
                if (!locked) {
                    Toast.makeText(this, "Enable Accessibility service to lock screen", Toast.LENGTH_LONG).show()
                }
            }
            is VoiceCommandResult.LaunchApp -> {
                val launched = AppLauncherHelper.launchAppByName(this, result.appName)
                if (!launched) {
                    Toast.makeText(this, "App '${result.appName}' not found", Toast.LENGTH_SHORT).show()
                }
            }
            is VoiceCommandResult.UnlockScreen -> {
                Toast.makeText(this, "Screen unlocked", Toast.LENGTH_SHORT).show()
            }
            is VoiceCommandResult.Unknown -> {
                // Try opening as app
                val launched = AppLauncherHelper.launchAppByName(this, result.rawText)
                if (!launched) {
                    Toast.makeText(this, "Heard: '${result.rawText}'", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
        }
    }
}
