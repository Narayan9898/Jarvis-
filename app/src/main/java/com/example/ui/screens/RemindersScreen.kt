package com.example.ui.screens

import android.content.Intent
import android.provider.AlarmClock
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.ReminderEntity
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant

@Composable
fun RemindersScreen(
    reminders: List<ReminderEntity>,
    onAddReminder: (title: String, type: String, time: String) -> Unit,
    onCompleteReminder: (Long) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = "Alarms & Reminders",
                    tint = JarvisCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ALARMS & REMINDERS",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // clock app intent unavailable
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyan)
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = "System Clock", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("System Clock", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFF456183),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No active alarms or reminders.",
                        color = Color(0xFF869DBE),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Tell JARVIS: 'Set alarm for 6 AM' or tap below.",
                        color = Color(0xFF536E91),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reminders, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (item.isCompleted) Color(0xFF0C1420) else JarvisSurface)
                            .border(
                                1.dp,
                                if (item.isCompleted) Color(0xFF132338) else Color(0xFF1B3050),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (item.type == "ALARM") JarvisAmber.copy(alpha = 0.2f)
                                            else JarvisCyan.copy(alpha = 0.2f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.type,
                                        color = if (item.type == "ALARM") JarvisAmber else JarvisCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.timeFormatted,
                                    color = Color(0xFFA5B8D1),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.title,
                                color = if (item.isCompleted) Color(0xFF677E9C) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!item.isCompleted) {
                                IconButton(onClick = { onCompleteReminder(item.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Mark Complete",
                                        tint = JarvisGreen
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteReminder(item.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = JarvisRed.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_reminder_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = JarvisCyan,
                contentColor = Color(0xFF00222A)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create New Reminder / Alarm", fontWeight = FontWeight.Bold)
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("REMINDER") }
        var time by remember { mutableStateOf("08:00 AM") }

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
                        text = "NEW SCHEDULED TASK",
                        color = JarvisCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task or Reminder Description") },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("REMINDER", "ALARM", "TIMER").forEach { option ->
                            Button(
                                onClick = { type = option },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (type == option) JarvisCyan else JarvisSurfaceVariant,
                                    contentColor = if (type == option) Color(0xFF00222A) else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(option, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time (e.g. 08:00 AM, Tomorrow)") },
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
                                if (title.isNotBlank()) {
                                    onAddReminder(title, type, time)
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = Color(0xFF00222A)
                            )
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
