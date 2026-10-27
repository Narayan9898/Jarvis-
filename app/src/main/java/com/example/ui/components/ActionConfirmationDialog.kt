package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ActionProposal
import com.example.data.model.RiskLevel
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant

@Composable
fun ActionConfirmationDialog(
    proposal: ActionProposal,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .testTag("action_confirmation_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = JarvisSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with Shield
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Authorization",
                            tint = JarvisCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SECURITY PROTOCOL",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    val (riskLabel, riskColor) = when (proposal.riskLevel) {
                        RiskLevel.LOW -> Pair("LOW RISK", JarvisCyan)
                        RiskLevel.MEDIUM -> Pair("MEDIUM RISK", JarvisAmber)
                        RiskLevel.HIGH -> Pair("HIGH RISK", JarvisRed)
                    }

                    Box(
                        modifier = Modifier
                            .background(riskColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .border(1.dp, riskColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = riskLabel,
                            color = riskColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = proposal.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = proposal.description,
                    color = Color(0xFFA5B4CB),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (proposal.parameters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "PARAMETERS:",
                        color = Color(0xFF7E94B5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(JarvisSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        proposal.parameters.forEach { (key, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = key,
                                    color = JarvisCyan,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = value,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_cancel_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFB0C4DE)
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.padding(end = 4.dp))
                        Text("Abort")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("action_confirm_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCyan,
                            contentColor = Color(0xFF00222A)
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Authorize", modifier = Modifier.padding(end = 4.dp))
                        Text("Authorize", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
