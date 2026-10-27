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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.MemoryEntity
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant

@Composable
fun MemoryScreen(
    memories: List<MemoryEntity>,
    onAddMemory: (category: String, key: String, value: String) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onClearAllMemory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

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
                    imageVector = Icons.Default.Memory,
                    contentDescription = "Personal Memory",
                    tint = JarvisCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PERSONAL MEMORY BANK",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            if (memories.isNotEmpty()) {
                IconButton(onClick = onClearAllMemory) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear All Memories",
                        tint = JarvisRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "User-approved memories help JARVIS personalize responses locally. Stored strictly on device.",
            color = Color(0xFF869DBE),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(memories, key = { it.id }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(JarvisSurface)
                        .border(1.dp, Color(0xFF1A2F4B), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .background(JarvisCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.category,
                                color = JarvisCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.key.replace('_', ' ').uppercase(),
                            color = Color(0xFFA5B8D1),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = item.value,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(onClick = { onDeleteMemory(item.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Memory",
                            tint = Color(0xFF6E87A8)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_memory_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = JarvisCyan,
                contentColor = Color(0xFF00222A)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Memory Fact", fontWeight = FontWeight.Bold)
        }
    }

    if (showAddDialog) {
        var key by remember { mutableStateOf("") }
        var value by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("PREFERENCE") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = JarvisSurface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "NEW MEMORY ENTRY",
                        color = JarvisCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text("Fact Key (e.g. favorite_city, nickname)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = Color(0xFF1E3558),
                            focusedContainerColor = JarvisSurfaceVariant,
                            unfocusedContainerColor = JarvisSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Memory Value") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = Color(0xFF1E3558),
                            focusedContainerColor = JarvisSurfaceVariant,
                            unfocusedContainerColor = JarvisSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { showAddDialog = false }) {
                            Text("Cancel", color = Color(0xFF869DBE))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (key.isNotBlank() && value.isNotBlank()) {
                                    onAddMemory(category, key.trim(), value.trim())
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = Color(0xFF00222A)
                            )
                        ) {
                            Text("Save Memory", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
