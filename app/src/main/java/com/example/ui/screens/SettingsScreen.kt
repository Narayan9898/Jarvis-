package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant

@Composable
fun SettingsScreen(
    currentApiKey: String,
    onSaveApiKey: (String) -> Unit,
    onUpdateVoiceSettings: (rate: Float, pitch: Float) -> Unit,
    onTestVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var speechPitch by remember { mutableFloatStateOf(1.0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = JarvisCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SYSTEM CONFIGURATION",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Google Gemini AI API Configuration
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisSurface)
                .border(1.dp, Color(0xFF1B3150), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cloud, contentDescription = null, tint = JarvisCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GEMINI AI ENGINE",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val hasConfiguredKey = currentApiKey.isNotBlank() && currentApiKey != "MY_GEMINI_API_KEY"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(
                            if (hasConfiguredKey) JarvisGreen.copy(alpha = 0.2f) else JarvisAmber.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (hasConfiguredKey) "API KEY ACTIVE" else "OFFLINE / LOCAL NLU FALLBACK",
                        color = if (hasConfiguredKey) JarvisGreen else JarvisAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Configure your Google Gemini API key or use the AI Studio Secrets panel. When no key is provided, JARVIS continues operating via local multilingual pattern matching.",
                color = Color(0xFFA5B8D1),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("Gemini API Key") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = Color(0xFF1E3558),
                    focusedContainerColor = JarvisSurfaceVariant,
                    unfocusedContainerColor = JarvisSurfaceVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { onSaveApiKey(apiKeyInput.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisCyan,
                    contentColor = Color(0xFF00222A)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_api_key_button")
            ) {
                Text("Update API Key", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Voice & Speech Synthesis Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisSurface)
                .border(1.dp, Color(0xFF1B3150), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = JarvisCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SPEECH SYNTHESIS CALIBRATION",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Speech Rate: %.1fx".format(speechRate),
                color = Color(0xFFA5B8D1),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Slider(
                value = speechRate,
                onValueChange = {
                    speechRate = it
                    onUpdateVoiceSettings(speechRate, speechPitch)
                },
                valueRange = 0.5f..1.8f,
                colors = SliderDefaults.colors(
                    thumbColor = JarvisCyan,
                    activeTrackColor = JarvisCyan
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Speech Pitch: %.1fx".format(speechPitch),
                color = Color(0xFFA5B8D1),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Slider(
                value = speechPitch,
                onValueChange = {
                    speechPitch = it
                    onUpdateVoiceSettings(speechRate, speechPitch)
                },
                valueRange = 0.5f..1.8f,
                colors = SliderDefaults.colors(
                    thumbColor = JarvisCyan,
                    activeTrackColor = JarvisCyan
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onTestVoice,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisSurfaceVariant,
                    contentColor = JarvisCyan
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("test_voice_button")
            ) {
                Text("Test Voice Synthesis", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Security & Production Architecture Reference
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisSurface)
                .border(1.dp, Color(0xFF1B3150), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = JarvisGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SECURITY & ARCHITECTURE",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = """
                    • Action Allowlist: All actions are validated against strict JSON schemas before execution.
                    • Dangerous OS Calls: High and medium-risk actions require user confirmation.
                    • Production Setup: In production, Android apps connect to an authenticated backend (e.g. Node.js or Kotlin Ktor) which enforces rate limits and keeps Gemini API keys secure.
                    • Multilingual: Telugu, English, Hindi, and Tanglish queries are supported.
                """.trimIndent(),
                color = Color(0xFF869DBE),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
