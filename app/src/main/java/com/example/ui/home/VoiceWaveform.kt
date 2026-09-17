package com.example.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.core.voice.AssistantState
import com.example.ui.theme.RexyyBlue
import com.example.ui.theme.RexyyCyan
import com.example.ui.theme.RexyyEmerald

@Composable
fun VoiceWaveform(
    state: AssistantState,
    rmsDb: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 18
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_pulse"
    )

    val isActive = state == AssistantState.LISTENING || state == AssistantState.SPEAKING
    val baseScale = if (isActive) (rmsDb / 8f).coerceIn(0.2f, 1f) else 0.08f

    Row(
        modifier = modifier.height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barBrushes = listOf(
            Brush.verticalGradient(listOf(RexyyCyan, RexyyBlue)),
            Brush.verticalGradient(listOf(RexyyBlue, RexyyEmerald)),
            Brush.verticalGradient(listOf(RexyyEmerald, RexyyCyan))
        )

        for (i in 0 until barCount) {
            // Wave curvature
            val normalizedIdx = kotlin.math.abs(i - barCount / 2f) / (barCount / 2f)
            val bellCurve = 1f - (normalizedIdx * 0.5f)

            val animatedHeight = if (isActive) {
                val waveOffset = kotlin.math.sin(i * 0.65f + pulseFactor * 5f)
                val dynamicHeight = (baseScale * 30f * bellCurve + waveOffset * 6f).coerceIn(4f, 34f)
                dynamicHeight.dp
            } else {
                4.dp
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(animatedHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isActive) barBrushes[i % barBrushes.size]
                        else Brush.verticalGradient(listOf(Color(0xFF1E2D48), Color(0xFF131C2D)))
                    )
            )
        }
    }
}
