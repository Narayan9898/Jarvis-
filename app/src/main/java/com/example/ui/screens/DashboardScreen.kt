package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ActionProposal
import com.example.data.model.JarvisState
import com.example.ui.components.ActionConfirmationDialog
import com.example.ui.components.ArcReactorCore
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant

@Composable
fun DashboardScreen(
    jarvisState: JarvisState,
    statusText: String,
    partialSpeech: String,
    rmsVolume: Float,
    isSpeaking: Boolean,
    isContinuousListening: Boolean,
    pendingAction: ActionProposal?,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onToggleContinuousListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onSendText: (String) -> Unit,
    onConfirmAction: (ActionProposal) -> Unit,
    onCancelAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartVoice()
        }
    }

    val continuousPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onToggleContinuousListening()
        }
    }

    val handleMicClick = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            if (jarvisState == JarvisState.LISTENING && !isContinuousListening) {
                onStopVoice()
            } else {
                onStartVoice()
            }
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val handleContinuousToggle = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            onToggleContinuousListening()
        } else {
            continuousPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Confirmation dialog if an action requires authorization
    if (pendingAction != null) {
        ActionConfirmationDialog(
            proposal = pendingAction,
            onConfirm = { onConfirmAction(pendingAction) },
            onDismiss = onCancelAction
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hologram HUD status header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(JarvisSurfaceVariant, JarvisSurface)
                    )
                )
                .border(1.dp, Color(0xFF162B47), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CORE STATUS",
                        color = JarvisCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = when (jarvisState) {
                            JarvisState.IDLE -> "STANDBY // READY"
                            JarvisState.LISTENING -> if (isContinuousListening) "CONTINUOUS LISTENING // ON" else "AUDIO INPUT ACTIVE"
                            JarvisState.THINKING -> "PROCESSING INTENT WITH GEMINI..."
                            JarvisState.SPEAKING -> "SYNTHESIZING SPEECH"
                            JarvisState.EXECUTING -> "RUNNING OS PROTOCOL"
                            JarvisState.CONFIRMATION_REQUIRED -> "AWAITING AUTHORIZATION"
                            JarvisState.ERROR -> "SYSTEM ALERT"
                        },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSpeaking) {
                        IconButton(
                            onClick = onStopSpeaking,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(JarvisRed.copy(alpha = 0.2f))
                                .testTag("stop_speaking_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Speaking",
                                tint = JarvisRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hands-Free / Continuous Listening Mode Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isContinuousListening) Color(0xFF0C243B) else JarvisSurface)
                .border(
                    1.dp,
                    if (isContinuousListening) JarvisCyan else Color(0xFF1B3050),
                    RoundedCornerShape(10.dp)
                )
                .clickable { handleContinuousToggle() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("toggle_continuous_listening"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = if (isContinuousListening) JarvisGreen else Color(0xFF6B83A8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "CONTINUOUS LISTENING (HANDS-FREE)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isContinuousListening)
                            "Listening continuously for commands without button presses"
                        else
                            "Tap to enable always-on voice command detection",
                        color = if (isContinuousListening) JarvisCyanBright else Color(0xFF758CA8),
                        fontSize = 10.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isContinuousListening) JarvisGreen.copy(alpha = 0.25f)
                        else Color(0xFF16253A)
                    )
                    .border(
                        1.dp,
                        if (isContinuousListening) JarvisGreen else Color(0xFF263D5C),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isContinuousListening) "ACTIVE" else "OFF",
                    color = if (isContinuousListening) JarvisGreen else Color(0xFF869DBE),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center Animated Arc Reactor Core
        ArcReactorCore(
            state = jarvisState,
            rmsVolume = rmsVolume,
            onClick = handleMicClick,
            size = 230.dp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Real-time audio waveform visualizer
        WaveformVisualizer(
            isActive = jarvisState == JarvisState.LISTENING || isSpeaking,
            volume = if (jarvisState == JarvisState.LISTENING) rmsVolume else 0.5f
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Live Speech / Assistant Status Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisSurface)
                .border(1.dp, Color(0xFF1D3558), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (partialSpeech.isNotBlank()) "TRANSCRIPT (LIVE):" else "INTELLIGENCE LOG:",
                    color = JarvisCyanBright,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (partialSpeech.isNotBlank()) partialSpeech else statusText,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Suggestion Chips (Multilingual: English, Telugu, Tanglish)
        Text(
            text = "QUICK COMMANDS //",
            color = Color(0xFF869DBE),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "Open YouTube",
                "జార్విస్, YouTube ఓపెన్ చేయి",
                "Set alarm for 6 AM",
                "రేపు ఉదయం 6 గంటలకు అలారం పెట్టు",
                "Remind me to pay bills",
                "Open Wi-Fi settings",
                "Turn on flashlight",
                "Call Dad",
                "Activate focus mode",
                "Explain this in Telugu"
            )

            suggestions.forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(JarvisSurfaceVariant)
                        .border(1.dp, Color(0xFF1F3A60), RoundedCornerShape(20.dp))
                        .clickable { onSendText(suggestion) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = suggestion,
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Text Command Input Bar & Mic Push-To-Talk
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                placeholder = {
                    Text(
                        text = if (isContinuousListening) "Listening hands-free..." else "Speak or type command...",
                        color = Color(0xFF6B83A8),
                        fontSize = 13.sp
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = Color(0xFF1E3558),
                    focusedContainerColor = JarvisSurface,
                    unfocusedContainerColor = JarvisSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (textInput.isNotBlank()) {
                            onSendText(textInput.trim())
                            textInput = ""
                        }
                    }
                ),
                trailingIcon = {
                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                onSendText(textInput.trim())
                                textInput = ""
                            },
                            modifier = Modifier.testTag("send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Command",
                                tint = JarvisCyan
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Push-To-Talk Mic FAB
            FloatingActionButton(
                onClick = handleMicClick,
                containerColor = when {
                    isContinuousListening -> JarvisGreen
                    jarvisState == JarvisState.LISTENING -> JarvisRed
                    else -> JarvisCyan
                },
                contentColor = when {
                    isContinuousListening -> Color(0xFF00222A)
                    jarvisState == JarvisState.LISTENING -> Color.White
                    else -> Color(0xFF00222A)
                },
                shape = CircleShape,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("push_to_talk_button")
            ) {
                Icon(
                    imageVector = when {
                        isContinuousListening -> Icons.Default.Hearing
                        jarvisState == JarvisState.LISTENING -> Icons.Default.MicOff
                        else -> Icons.Default.Mic
                    },
                    contentDescription = if (jarvisState == JarvisState.LISTENING) "Stop Microphone" else "Start Microphone"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
