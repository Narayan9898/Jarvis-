package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    isActive: Boolean,
    volume: Float = 0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val barCount = 32
        val barWidth = 4.dp.toPx()
        val spacing = (size.width - (barCount * barWidth)) / (barCount - 1)
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val x = i * (barWidth + spacing)
            val normalizedIndex = i.toFloat() / barCount.toFloat()

            val wave = sin((normalizedIndex * 4 * Math.PI + phase).toDouble()).toFloat()
            val baseHeight = if (isActive) {
                val amplitude = 8.dp.toPx() + (volume * 24.dp.toPx())
                (amplitude * Math.abs(wave)).coerceIn(4.dp.toPx(), size.height)
            } else {
                3.dp.toPx()
            }

            val topY = centerY - (baseHeight / 2f)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (isActive) {
                        listOf(JarvisCyan, JarvisElectricBlue)
                    } else {
                        listOf(Color(0xFF1E3557), Color(0xFF102035))
                    }
                ),
                topLeft = Offset(x, topY),
                size = Size(barWidth, baseHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}
