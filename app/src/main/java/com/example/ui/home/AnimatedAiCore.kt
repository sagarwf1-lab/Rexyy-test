package com.example.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.voice.AssistantState
import com.example.ui.theme.RexyyBlue
import com.example.ui.theme.RexyyCyan
import com.example.ui.theme.RexyyEmerald
import com.example.ui.theme.RexyyError
import com.example.ui.theme.RexyyWarning
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedAiCore(
    state: AssistantState,
    rmsDb: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "core_anim")

    // Continuous rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING -> 2000
                    AssistantState.LISTENING -> 4000
                    AssistantState.SPEAKING -> 3000
                    else -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.LISTENING -> 600
                    AssistantState.SPEAKING -> 800
                    AssistantState.THINKING -> 700
                    else -> 2200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Audio amplitude reactive factor
    val dynamicAmplitude = (rmsDb / 10f).coerceIn(0f, 1f)
    val effectiveScale = (breathingPulse + dynamicAmplitude * 0.35f).coerceIn(0.8f, 1.45f)

    val primaryColor = when (state) {
        AssistantState.LISTENING -> RexyyCyan
        AssistantState.THINKING -> RexyyBlue
        AssistantState.SPEAKING -> RexyyEmerald
        AssistantState.ERROR -> RexyyError
        AssistantState.IDLE -> RexyyCyan
    }

    val secondaryColor = when (state) {
        AssistantState.LISTENING -> RexyyBlue
        AssistantState.THINKING -> RexyyCyan
        AssistantState.SPEAKING -> RexyyCyan
        AssistantState.ERROR -> RexyyWarning
        AssistantState.IDLE -> RexyyBlue
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("ai_core_button")
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.72f

            // 1. Ambient Glow Gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f * effectiveScale),
                        secondaryColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.35f * effectiveScale
                ),
                radius = baseRadius * 1.35f * effectiveScale,
                center = center
            )

            // 2. Outer Orbiting Energy Arcs
            rotate(rotation, pivot = center) {
                // Outer ring track
                drawCircle(
                    color = primaryColor.copy(alpha = 0.15f),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Outer rotating arcs
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(primaryColor, secondaryColor, Color.Transparent)
                    ),
                    startAngle = 0f,
                    sweepAngle = 100f,
                    useCenter = false,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(secondaryColor, primaryColor, Color.Transparent)
                    ),
                    startAngle = 180f,
                    sweepAngle = 110f,
                    useCenter = false,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Counter-rotating Intermediate Ring
            rotate(-rotation * 1.4f, pivot = center) {
                val midRadius = baseRadius * 0.75f
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.2f),
                    radius = midRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Segmented tick marks
                val segments = 12
                for (i in 0 until segments) {
                    val angle = (i * 360f / segments) * (Math.PI / 180f)
                    val r1 = midRadius - 4.dp.toPx()
                    val r2 = midRadius + 4.dp.toPx()
                    val start = Offset(
                        center.x + (r1 * cos(angle)).toFloat(),
                        center.y + (r1 * sin(angle)).toFloat()
                    )
                    val end = Offset(
                        center.x + (r2 * cos(angle)).toFloat(),
                        center.y + (r2 * sin(angle)).toFloat()
                    )
                    drawLine(
                        color = secondaryColor.copy(alpha = 0.45f),
                        start = start,
                        end = end,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                drawArc(
                    color = primaryColor.copy(alpha = 0.85f),
                    startAngle = 45f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = secondaryColor.copy(alpha = 0.85f),
                    startAngle = 225f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Central Glowing Orb (Core)
            val coreRadius = baseRadius * 0.44f * effectiveScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        secondaryColor,
                        primaryColor.copy(alpha = 0.2f)
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // 5. High-intensity Core Border
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = coreRadius * 0.55f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
