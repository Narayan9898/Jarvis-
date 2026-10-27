package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JarvisState
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorCore(
    state: JarvisState,
    rmsVolume: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_anim")

    // Outer slow rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = when (state) {
                JarvisState.THINKING -> 3000
                JarvisState.LISTENING -> 6000
                else -> 14000
            }, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )

    // Inner reverse rotation
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = when (state) {
                JarvisState.THINKING -> 2000
                JarvisState.SPEAKING -> 4000
                else -> 10000
            }, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )

    // Core pulsing glow
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = when (state) {
                JarvisState.LISTENING -> 600
                JarvisState.SPEAKING -> 800
                JarvisState.THINKING -> 500
                else -> 1500
            }, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    // Primary state colors
    val (primaryGlow, accentGlow) = when (state) {
        JarvisState.IDLE -> Pair(JarvisCyan, JarvisElectricBlue)
        JarvisState.LISTENING -> Pair(JarvisCyanBright, Color(0xFF00E5FF))
        JarvisState.THINKING -> Pair(Color(0xFF82B1FF), JarvisElectricBlue)
        JarvisState.SPEAKING -> Pair(JarvisCyanBright, JarvisGreen)
        JarvisState.EXECUTING -> Pair(JarvisGreen, JarvisCyan)
        JarvisState.CONFIRMATION_REQUIRED -> Pair(JarvisAmber, Color(0xFFFF9100))
        JarvisState.ERROR -> Pair(JarvisRed, Color(0xFFFF5252))
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("arc_reactor_core")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = (this.size.minDimension / 2f) - 12.dp.toPx()

            // 1. Ambient Background Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.25f * corePulse),
                        accentGlow.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 1.1f
                ),
                radius = maxRadius * 1.1f,
                center = center
            )

            // 2. Outermost Metric Ring with Notch Marks
            rotate(outerRotation, pivot = center) {
                drawCircle(
                    color = primaryGlow.copy(alpha = 0.35f),
                    radius = maxRadius,
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 18f), 0f)
                    )
                )

                // 8 Outer reactor brackets
                val numBrackets = 8
                val bracketRadius = maxRadius - 6.dp.toPx()
                for (i in 0 until numBrackets) {
                    val angle = (i * (2 * PI / numBrackets)).toFloat()
                    val startX = center.x + (bracketRadius - 8.dp.toPx()) * cos(angle)
                    val startY = center.y + (bracketRadius - 8.dp.toPx()) * sin(angle)
                    val endX = center.x + bracketRadius * cos(angle)
                    val endY = center.y + bracketRadius * sin(angle)
                    drawLine(
                        color = primaryGlow,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. Middle Segmented Hologram Ring
            rotate(innerRotation, pivot = center) {
                val midRadius = maxRadius * 0.75f
                drawCircle(
                    color = accentGlow.copy(alpha = 0.6f),
                    radius = midRadius,
                    center = center,
                    style = Stroke(
                        width = 3.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f, 10f, 20f), 0f)
                    )
                )

                // Concentric inner arc nodes
                for (i in 0 until 4) {
                    val angle = (i * (2 * PI / 4)).toFloat()
                    val nodeRadius = midRadius - 8.dp.toPx()
                    val nx = center.x + nodeRadius * cos(angle)
                    val ny = center.y + nodeRadius * sin(angle)
                    drawCircle(
                        color = primaryGlow,
                        radius = 3.dp.toPx(),
                        center = Offset(nx, ny)
                    )
                }
            }

            // 4. Sound Energy Reactive Ring (Expands with voice volume)
            val volumeExpansion = rmsVolume * 16.dp.toPx()
            val reactiveRadius = (maxRadius * 0.52f) + volumeExpansion
            drawCircle(
                color = primaryGlow.copy(alpha = 0.7f),
                radius = reactiveRadius,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // 5. Center Arc Reactor AI Nucleus
            val nucleusRadius = maxRadius * 0.32f * corePulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryGlow,
                        accentGlow.copy(alpha = 0.6f)
                    ),
                    center = center,
                    radius = nucleusRadius
                ),
                radius = nucleusRadius,
                center = center
            )
        }

        // Center HUD State Label
        Text(
            text = when (state) {
                JarvisState.IDLE -> "JARVIS"
                JarvisState.LISTENING -> "LISTENING"
                JarvisState.THINKING -> "COMPUTING"
                JarvisState.SPEAKING -> "SPEAKING"
                JarvisState.EXECUTING -> "EXECUTING"
                JarvisState.CONFIRMATION_REQUIRED -> "CONFIRM"
                JarvisState.ERROR -> "ALERT"
            },
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )
    }
}
