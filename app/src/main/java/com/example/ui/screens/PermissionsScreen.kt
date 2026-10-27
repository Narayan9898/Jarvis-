package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant

data class PermissionItem(
    val title: String,
    val permission: String,
    val description: String,
    val dataAccessed: String,
    val requiredForFeature: String
)

@Composable
fun PermissionsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }

    val permissionItems = remember {
        listOf(
            PermissionItem(
                title = "Microphone Access",
                permission = Manifest.permission.RECORD_AUDIO,
                description = "Captures user voice audio for speech recognition.",
                dataAccessed = "Microphone raw audio stream during push-to-talk.",
                requiredForFeature = "Push-to-talk voice commands in English, Telugu, and Hindi."
            ),
            PermissionItem(
                title = "Contacts Directory",
                permission = Manifest.permission.READ_CONTACTS,
                description = "Searches phonebook to prepare phone calls or SMS.",
                dataAccessed = "Device contact names and phone numbers.",
                requiredForFeature = "'Call Dad' or 'Message Ravi' commands."
            ),
            PermissionItem(
                title = "Phone Calling",
                permission = Manifest.permission.CALL_PHONE,
                description = "Allows initiating calls to contacts via phone dialer.",
                dataAccessed = "Telephony call manager.",
                requiredForFeature = "Direct dial execution."
            ),
            PermissionItem(
                title = "Calendar Access",
                permission = Manifest.permission.READ_CALENDAR,
                description = "Reads schedule for daily briefings and alarms.",
                dataAccessed = "Calendar event titles, start/end dates.",
                requiredForFeature = "Morning routines & schedule inquiry."
            ),
            PermissionItem(
                title = "Camera & Flashlight",
                permission = Manifest.permission.CAMERA,
                description = "Enables hardware flashlight torch & viewfinder.",
                dataAccessed = "Camera sensor & flashlight LED hardware.",
                requiredForFeature = "'Turn on torch' and image analysis."
            ),
            PermissionItem(
                title = "Notifications",
                permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else "",
                description = "Shows alerts for scheduled reminders and timers.",
                dataAccessed = "System notification shade.",
                requiredForFeature = "Alarm and task completion notifications."
            )
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshKey++
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Permissions",
                    tint = JarvisCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PERMISSIONS & PRIVACY",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyan)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "System Settings", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Settings", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "JARVIS requests least-privilege permissions on-demand. Revoke anytime in OS settings.",
            color = Color(0xFF869DBE),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(permissionItems) { item ->
                val isGranted = if (item.permission.isNotBlank()) {
                    ContextCompat.checkSelfPermission(context, item.permission) == PackageManager.PERMISSION_GRANTED
                } else true

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisSurface)
                        .border(1.dp, if (isGranted) Color(0xFF19324C) else Color(0xFF382329), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isGranted) JarvisGreen else JarvisAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isGranted) "AUTHORIZED" else "NOT GRANTED",
                                color = if (isGranted) JarvisGreen else JarvisAmber,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = item.description, color = Color(0xFFA5B8D1), fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Data: ${item.dataAccessed}",
                        color = Color(0xFF6B83A8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isGranted && item.permission.isNotBlank()) {
                        Button(
                            onClick = { launcher.launch(item.permission) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = Color(0xFF00222A)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("grant_permission_${item.title}")
                        ) {
                            Text("Grant Permission", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
