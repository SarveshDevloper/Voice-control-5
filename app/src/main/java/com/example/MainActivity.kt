package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.FloatingVoiceAssistantService
import com.example.service.VoiceAssistantAccessibilityService
import com.example.service.VoiceLockForegroundService
import com.example.ui.theme.*
import com.example.util.AppLauncherHelper
import com.example.util.VoiceCommandDispatcher
import com.example.util.VoiceCommandResult

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoiceScreenLockTheme {
                MainVoiceScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainVoiceScreen() {
    val context = LocalContext.current
    var isFloatingMicEnabled by remember { mutableStateOf(false) }
    var spokenCommandInput by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf("Ready to execute voice commands") }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            feedbackMessage = "Audio permission granted"
        } else {
            feedbackMessage = "Audio permission is required for voice control"
        }
    }

    LaunchedEffect(Unit) {
        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        val fgIntent = Intent(context, VoiceLockForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(fgIntent)
        } else {
            context.startService(fgIntent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice Mic",
                            tint = NeonCyan,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Voice Screen Lock", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = DeepNavy
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🎙️ Floating Voice Assistant",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Screen par floating mic dikhayein taaki bina app khole direct commands bol sakein.",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Floating Mic Overlay", color = Color.White, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isFloatingMicEnabled,
                                onCheckedChange = { enable ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                        feedbackMessage = "Please grant Display Over Other Apps permission"
                                    } else {
                                        isFloatingMicEnabled = enable
                                        val intent = Intent(context, FloatingVoiceAssistantService::class.java)
                                        if (enable) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                context.startForegroundService(intent)
                                            } else {
                                                context.startService(intent)
                                            }
                                            feedbackMessage = "Floating mic active! Tap it anytime to speak."
                                        } else {
                                            context.stopService(intent)
                                            feedbackMessage = "Floating mic stopped"
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("floating_mic_switch")
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "⚡ Voice Command Test",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricPurple
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Neeche command type karein ya test chips par tap karein:",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = spokenCommandInput,
                            onValueChange = { spokenCommandInput = it },
                            placeholder = { Text("e.g. Open WhatsApp, Lock Screen, Open Camera") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("command_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (spokenCommandInput.isNotBlank()) {
                                    when (val res = VoiceCommandDispatcher.parse(spokenCommandInput)) {
                                        is VoiceCommandResult.LockScreen -> {
                                            val ok = VoiceAssistantAccessibilityService.lockDevice()
                                            feedbackMessage = if (ok) "Screen lock executed!" else "Accessibility service chahiye lock karne ke liye"
                                        }
                                        is VoiceCommandResult.LaunchApp -> {
                                            val ok = AppLauncherHelper.launchAppByName(context, res.appName)
                                            feedbackMessage = if (ok) "Opening ${res.appName}..." else "App '${res.appName}' nahi mili"
                                        }
                                        is VoiceCommandResult.UnlockScreen -> {
                                            feedbackMessage = "Screen unlocked successfully!"
                                        }
                                        is VoiceCommandResult.Unknown -> {
                                            val ok = AppLauncherHelper.launchAppByName(context, res.rawText)
                                            feedbackMessage = if (ok) "Launching ${res.rawText}..." else "Heard: ${res.rawText}"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_command_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DeepNavy)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Command", color = DeepNavy, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🚀 Quick Test Shortcuts", fontWeight = FontWeight.Bold, color = NeonGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    spokenCommandInput = "Open Camera"
                                    AppLauncherHelper.launchAppByName(context, "camera")
                                    feedbackMessage = "Launching Camera..."
                                },
                                modifier = Modifier.testTag("test_camera_button")
                            ) {
                                Text("📷 Camera")
                            }

                            FilledTonalButton(
                                onClick = {
                                    spokenCommandInput = "Lock Screen"
                                    val ok = VoiceAssistantAccessibilityService.lockDevice()
                                    feedbackMessage = if (ok) "Locking screen..." else "Enable Accessibility Service in Android Settings"
                                },
                                modifier = Modifier.testTag("test_lock_button")
                            ) {
                                Text("🔒 Lock")
                            }

                            FilledTonalButton(
                                onClick = {
                                    spokenCommandInput = "Open Browser"
                                    AppLauncherHelper.launchAppByName(context, "browser")
                                    feedbackMessage = "Opening Browser..."
                                },
                                modifier = Modifier.testTag("test_browser_button")
                            ) {
                                Text("🌐 Browser")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2648))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            feedbackMessage,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
