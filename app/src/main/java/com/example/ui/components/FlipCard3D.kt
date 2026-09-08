package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import com.example.ui.theme.CardLightEdge
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated

@Composable
fun FlipCard3D(
    isFlipped: Boolean,
    modifier: Modifier = Modifier,
    front: @Composable BoxScope.() -> Unit,
    back: @Composable BoxScope.() -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardFlip"
    )

    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 18f * density
                shadowElevation = 10.dp.toPx()
            }
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = CardDarkEdge,
                spotColor = CardDarkEdge
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = if (rotation <= 90f) {
                        listOf(DarkSurfaceElevated, DarkSurface, Color(0xFF0F172A))
                    } else {
                        listOf(Color(0xFF1E1B4B), DarkSurface, DarkSurfaceElevated)
                    },
                    start = Offset(0f, 0f),
                    end = Offset(400f, 600f)
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        CardLightEdge,
                        if (rotation <= 90f) Color(0x3306B6D4) else Color(0x33F59E0B),
                        Color(0x05FFFFFF)
                    )
                ),
                shape = shape
            )
    ) {
        if (rotation <= 90f) {
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
