package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantLanguage
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant

@Composable
fun JarvisTopBar(
    selectedLanguage: AssistantLanguage,
    onLanguageSelected: (AssistantLanguage) -> Unit,
    onSettingsClick: () -> Unit,
    onPermissionsClick: () -> Unit,
    hasApiKey: Boolean,
    modifier: Modifier = Modifier
) {
    var showLangMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(JarvisSurface)
            .border(width = 0.5.dp, color = Color(0xFF16253D))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Branding & Online status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (hasApiKey) JarvisGreen else JarvisCyan)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "JARVIS AI",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (hasApiKey) "CLOUD GEMINI CONNECTED" else "LOCAL SECURE NLU",
                    color = if (hasApiKey) JarvisGreen else JarvisCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Right Controls: Language Selector, Permissions, Settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Language selector button
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisSurfaceVariant)
                        .border(1.dp, Color(0xFF1F3559), RoundedCornerShape(8.dp))
                        .clickable { showLangMenu = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = JarvisCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedLanguage.code.uppercase(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                DropdownMenu(
                    expanded = showLangMenu,
                    onDismissRequest = { showLangMenu = false }
                ) {
                    AssistantLanguage.values().forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = lang.displayName,
                                    fontWeight = if (lang == selectedLanguage) FontWeight.Bold else FontWeight.Normal,
                                    color = if (lang == selectedLanguage) JarvisCyan else Color.White
                                )
                            },
                            onClick = {
                                onLanguageSelected(lang)
                                showLangMenu = false
                            }
                        )
                    }
                }
            }

            IconButton(
                onClick = onPermissionsClick,
                modifier = Modifier.testTag("permissions_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Permissions Dashboard",
                    tint = JarvisCyan
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFFA5B4CB)
                )
            }
        }
    }
}
