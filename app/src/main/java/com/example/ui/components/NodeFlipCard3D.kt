package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CardDarkEdge
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NodeCharcoal
import com.example.ui.theme.NodeDarkBg
import com.example.ui.theme.NodeGreen
import com.example.ui.theme.NodeLime

/**
 * 3D Node.js Style Flip Card with ambient 3D tilt oscillation and spring-loaded 180° flip.
 */
@Composable
fun NodeFlipCard3D(
    isFlipped: Boolean,
    modifier: Modifier = Modifier,
    front: @Composable BoxScope.() -> Unit,
    back: @Composable BoxScope.() -> Unit
) {
    // 180-degree flip animation with bouncy spring
    val flipRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "nodeCardFlip"
    )

    // Ambient gentle 3D tilt to give constant floating depth
    val infiniteTransition = rememberInfiniteTransition(label = "ambientNodeTilt")
    val tiltY by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientTiltY"
    )
    val tiltX by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = -1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientTiltX"
    )

    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationX = tiltX
                rotationY = flipRotation + (if (flipRotation == 0f) tiltY else 0f)
                cameraDistance = 18f * density
                shadowElevation = 10.dp.toPx()
            }
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = CardDarkEdge,
                spotColor = if (flipRotation <= 90f) NodeGreen.copy(alpha = 0.25f) else Color(0x40F59E0B)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = if (flipRotation <= 90f) {
                        listOf(
                            NodeDarkBg,
                            DarkSurfaceElevated,
                            DarkSurface
                        )
                    } else {
                        listOf(
                            Color(0xFF19251B),
                            DarkSurface,
                            Color(0xFF0F172A)
                        )
                    },
                    start = Offset(0f, 0f),
                    end = Offset(400f, 600f)
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (flipRotation <= 90f) NodeGreen.copy(alpha = 0.6f) else Color(0x66F59E0B),
                        if (flipRotation <= 90f) NodeLime.copy(alpha = 0.25f) else Color(0x3306B6D4),
                        Color(0x05FFFFFF)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(300f, 300f)
                ),
                shape = shape
            )
    ) {
        if (flipRotation <= 90f) {
            Box(modifier = Modifier.fillMaxWidth()) {
                front()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        // Invert rotation so content on the back is upright
                        rotationY = 180f
                    }
            ) {
                back()
            }
        }
    }
}
