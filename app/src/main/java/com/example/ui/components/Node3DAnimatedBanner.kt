package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NodeCharcoal
import com.example.ui.theme.NodeDarkBg
import com.example.ui.theme.NodeGreen
import com.example.ui.theme.NodeGreenGlow
import com.example.ui.theme.NodeLime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive 3D Animated Node.js Type Hero Banner.
 * Features:
 * - Rotating 3D wireframe Hexagon / Node Prism with perspective projection
 * - Particle network / animated event loop nodes orbiting the core
 * - Node.js terminal prompt with runtime metrics and live reactive pulse
 * - Dark cybernetic aesthetic with Node green (#68A063 / #339933) accents
 */
@Composable
fun Node3DAnimatedBanner(
    modifier: Modifier = Modifier,
    totalProducts: Int = 0,
    totalCategories: Int = 0,
    onNodeClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "node3DLoop")

    // Rotation angle around Y axis
    val rotY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotY"
    )

    // Wobble angle around X axis
    val tiltX by infiniteTransition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tiltX"
    )

    // Breathing pulse
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NodeDarkBg,
                        NodeCharcoal,
                        Color(0xFF0C140F)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        NodeGreen.copy(alpha = 0.7f),
                        NodeGreenGlow.copy(alpha = 0.3f),
                        Color(0x1A000000)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        // Node Grid & Floating Matrix Canvas
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = 0.25f
                }
        ) {
            val gridStep = 24.dp.toPx()
            val w = size.width
            val h = size.height
            var x = 0f
            while (x <= w) {
                drawLine(
                    color = NodeGreen.copy(alpha = 0.12f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
                x += gridStep
            }
            var y = 0f
            while (y <= h) {
                drawLine(
                    color = NodeGreen.copy(alpha = 0.12f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Node.js Terminal Status & Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(NodeGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NODE.JS RUNTIME v20.12",
                        color = NodeLime,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Event-Loop MRP Engine",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Terminal command simulation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B100D))
                        .border(1.dp, Color(0xFF1B2E1E), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = NodeGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$ node mrp-server.js --active",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFD1D5DB)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Metric Tags
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricPill(label = "Items", value = totalProducts.toString())
                    Spacer(modifier = Modifier.width(6.dp))
                    MetricPill(label = "Categories", value = totalCategories.toString())
                    Spacer(modifier = Modifier.width(6.dp))
                    MetricPill(label = "Engine", value = "Ready", isGreen = true)
                }
            }

            // Right: 3D Animated Node Hexagon Canvas
            Box(
                modifier = Modifier
                    .size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = size.width * 0.38f

                    val radY = (rotY * PI / 180.0).toFloat()
                    val radX = (tiltX * PI / 180.0).toFloat()

                    // Project 3D coordinate (x, y, z) onto 2D canvas with isometric perspective
                    fun project3D(x: Float, y: Float, z: Float): Offset {
                        // Rotation around Y
                        val x1 = x * cos(radY) + z * sin(radY)
                        val z1 = -x * sin(radY) + z * cos(radY)

                        // Rotation around X
                        val y2 = y * cos(radX) - z1 * sin(radX)
                        val z2 = y * sin(radX) + z1 * cos(radX)

                        val fov = 300f
                        val scale = fov / (fov + z2 + 100f)
                        return Offset(cx + x1 * scale, cy + y2 * scale)
                    }

                    // Node.js Hexagon vertices in 3D: Front layer (z = +depth) & Back layer (z = -depth)
                    val depth = radius * 0.45f
                    val numPoints = 6
                    val frontPoints = mutableListOf<Offset>()
                    val backPoints = mutableListOf<Offset>()

                    for (i in 0 until numPoints) {
                        val angle = (i * 60.0 * PI / 180.0).toFloat()
                        val vx = radius * cos(angle)
                        val vy = radius * sin(angle)
                        frontPoints.add(project3D(vx, vy, depth))
                        backPoints.add(project3D(vx, vy, -depth))
                    }

                    // Draw connecting depth edges (between front & back hexagons)
                    for (i in 0 until numPoints) {
                        drawLine(
                            color = NodeGreen.copy(alpha = 0.45f * glowPulse),
                            start = frontPoints[i],
                            end = backPoints[i],
                            strokeWidth = 2f
                        )
                    }

                    // Draw Back Hexagon Path
                    val backPath = Path().apply {
                        moveTo(backPoints[0].x, backPoints[0].y)
                        for (i in 1 until numPoints) {
                            lineTo(backPoints[i].x, backPoints[i].y)
                        }
                        close()
                    }
                    drawPath(
                        path = backPath,
                        color = NodeGreen.copy(alpha = 0.3f),
                        style = Stroke(width = 2f)
                    )

                    // Draw Front Hexagon Path
                    val frontPath = Path().apply {
                        moveTo(frontPoints[0].x, frontPoints[0].y)
                        for (i in 1 until numPoints) {
                            lineTo(frontPoints[i].x, frontPoints[i].y)
                        }
                        close()
                    }
                    drawPath(
                        path = frontPath,
                        color = NodeLime.copy(alpha = 0.9f),
                        style = Stroke(width = 3f)
                    )

                    // Core Node.js glowing sphere in center
                    val coreCenter = project3D(0f, 0f, 0f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                NodeLime,
                                NodeGreen.copy(alpha = 0.8f),
                                Color.Transparent
                            ),
                            center = coreCenter,
                            radius = 24f * glowPulse
                        ),
                        radius = 16f,
                        center = coreCenter
                    )

                    // Orbiting Electron / Event Loop Particle
                    val orbitAngle = (rotY * 2.2f * PI / 180.0).toFloat()
                    val orbitRadius = radius * 1.25f
                    val ox = orbitRadius * cos(orbitAngle)
                    val oz = orbitRadius * sin(orbitAngle)
                    val oy = (radius * 0.4f) * sin(orbitAngle * 2f)
                    val particlePos = project3D(ox, oy, oz)

                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = particlePos
                    )
                    drawCircle(
                        color = NodeGreenGlow.copy(alpha = 0.6f),
                        radius = 8f,
                        center = particlePos
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    isGreen: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF0F1A12),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGreen) NodeGreen.copy(alpha = 0.6f) else Color(0xFF223526)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF9CA3AF)
            )
            Text(
                text = value,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isGreen) NodeLime else Color.White
            )
        }
    }
}
