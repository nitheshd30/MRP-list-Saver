package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CardDarkEdge
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NodeCharcoal
import com.example.ui.theme.NodeDarkBg
import com.example.ui.theme.NodeGreen
import com.example.ui.theme.NodeLime

/**
 * 3D Node.js Hexagonal styled card container with interactive tilting and glowing border.
 */
@Composable
fun NodeCard3D(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    elevation: Dp = 8.dp,
    glowAccent: Color = NodeGreen,
    isInteractive: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nodeCard3D")
    val tiltY by infiniteTransition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tiltY"
    )
    val tiltX by infiniteTransition.animateFloat(
        initialValue = 1.4f,
        targetValue = -1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tiltX"
    )

    val cardShape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationX = if (isInteractive) tiltX else 0f
                rotationY = if (isInteractive) tiltY else 0f
                cameraDistance = 16f * density
                shadowElevation = elevation.toPx()
                this.shape = cardShape
                clip = false
            }
            .shadow(
                elevation = elevation,
                shape = cardShape,
                ambientColor = CardDarkEdge,
                spotColor = glowAccent.copy(alpha = 0.2f)
            )
            .clip(cardShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        NodeDarkBg,
                        DarkSurfaceElevated,
                        DarkSurface
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(400f, 600f)
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        glowAccent.copy(alpha = 0.6f),
                        glowAccent.copy(alpha = 0.2f),
                        Color(0x05FFFFFF)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(300f, 300f)
                ),
                shape = cardShape
            )
            .padding(1.dp)
    ) {
        content()
    }
}
