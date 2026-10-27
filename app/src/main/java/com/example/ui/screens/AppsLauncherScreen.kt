package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppInfo
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant

@Composable
fun AppsLauncherScreen(
    appsList: List<AppInfo>,
    onLaunchApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(searchQuery, appsList) {
        if (searchQuery.isBlank()) appsList
        else appsList.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = "Applications",
                tint = JarvisCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "INSTALLED APPLICATIONS",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${filteredApps.size} APPS",
                color = JarvisCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed application...", color = Color(0xFF6E87A8)) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = JarvisCyan)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("apps_search_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisCyan,
                unfocusedBorderColor = Color(0xFF1E3558),
                focusedContainerColor = JarvisSurface,
                unfocusedContainerColor = JarvisSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "Scanning installed packages..." else "No applications match '$searchQuery'",
                    color = Color(0xFF869DBE),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(JarvisSurface)
                            .border(1.dp, Color(0xFF1B2F4C), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = app.packageName,
                                color = Color(0xFF758CA8),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Settings details shortcut
                            IconButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${app.packageName}")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "App Settings",
                                    tint = Color(0xFF7E96B5)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Button(
                                onClick = { onLaunchApp(app.packageName) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JarvisCyan,
                                    contentColor = Color(0xFF00222A)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("launch_app_${app.packageName}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Launch", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Launch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
