package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.LaserCyan
import com.example.ui.theme.LaserCyanGlow
import com.example.ui.theme.NodeGreen
import com.example.ui.theme.NodeGreenGlow
import com.example.ui.theme.NodeLime
import com.example.util.BarcodeAnalyzer
import java.util.concurrent.Executors

@Composable
fun Scanner3DView(
    modifier: Modifier = Modifier,
    onBarcodeDetected: (String) -> Unit,
    onManualInputClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Laser scan animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserY"
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    setOnTouchListener { v, event ->
                        if (event.action == android.view.MotionEvent.ACTION_UP) {
                            try {
                                val factory = meteringPointFactory
                                val point = factory.createPoint(event.x, event.y)
                                val action = androidx.camera.core.FocusMeteringAction.Builder(
                                    point,
                                    androidx.camera.core.FocusMeteringAction.FLAG_AF or androidx.camera.core.FocusMeteringAction.FLAG_AE
                                ).build()
                                camera?.cameraControl?.startFocusAndMetering(action)
                                v.performClick()
                            } catch (_: Exception) {}
                        }
                        true
                    }
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    cameraExecutor,
                                    BarcodeAnalyzer { code ->
                                        // Vibrate feedback
                                        triggerHaptic(context)
                                        onBarcodeDetected(code)
                                    }
                                )
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()
                        val cam = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        camera = cam
                    } catch (_: Exception) {
                        // Camera binding failed or emulator environment
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 3D Holographic Reticle & Laser Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val scanBoxWidth = canvasWidth * 0.78f
            val scanBoxHeight = canvasHeight * 0.35f
            val left = (canvasWidth - scanBoxWidth) / 2f
            val top = (canvasHeight - scanBoxHeight) / 2f - 40f
            val right = left + scanBoxWidth
            val bottom = top + scanBoxHeight

            // Dark dimmed surround
            drawRect(
                color = Color(0x99000000),
                size = Size(canvasWidth, top)
            )
            drawRect(
                color = Color(0x99000000),
                topLeft = Offset(0f, bottom),
                size = Size(canvasWidth, canvasHeight - bottom)
            )
            drawRect(
                color = Color(0x99000000),
                topLeft = Offset(0f, top),
                size = Size(left, scanBoxHeight)
            )
            drawRect(
                color = Color(0x99000000),
                topLeft = Offset(right, top),
                size = Size(canvasWidth - right, scanBoxHeight)
            )

            // Scanning box border with Node Green neon glow
            drawRoundRect(
                color = NodeGreen.copy(alpha = 0.45f),
                topLeft = Offset(left, top),
                size = Size(scanBoxWidth, scanBoxHeight),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = 2f)
            )

            // 4 Corner 3D Targeting Brackets in Node Lime
            val bracketLen = 36f
            val bracketStroke = 5f
            val bracketColor = NodeLime

            // Top-Left
            drawLine(bracketColor, Offset(left, top + bracketLen), Offset(left, top), bracketStroke)
            drawLine(bracketColor, Offset(left, top), Offset(left + bracketLen, top), bracketStroke)

            // Top-Right
            drawLine(bracketColor, Offset(right - bracketLen, top), Offset(right, top), bracketStroke)
            drawLine(bracketColor, Offset(right, top), Offset(right, top + bracketLen), bracketStroke)

            // Bottom-Left
            drawLine(bracketColor, Offset(left, bottom - bracketLen), Offset(left, bottom), bracketStroke)
            drawLine(bracketColor, Offset(left, bottom), Offset(left + bracketLen, bottom), bracketStroke)

            // Bottom-Right
            drawLine(bracketColor, Offset(right - bracketLen, bottom), Offset(right, bottom), bracketStroke)
            drawLine(bracketColor, Offset(right, bottom), Offset(right, bottom - bracketLen), bracketStroke)

            // Animated 3D Laser Plane (Node Lime & Emerald glow)
            val laserY = top + (scanBoxHeight * laserProgress)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x0068A063),
                        Color(0x3368A063),
                        NodeGreen
                    ),
                    startY = laserY - 35f,
                    endY = laserY
                ),
                topLeft = Offset(left + 4f, laserY - 35f),
                size = Size(scanBoxWidth - 8f, 35f)
            )
            drawLine(
                color = Color.White,
                start = Offset(left + 6f, laserY),
                end = Offset(right - 6f, laserY),
                strokeWidth = 3.5f
            )
        }

        // Top Controls: Title, Flash Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xCC121B14),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NodeGreen.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scanner",
                        tint = NodeLime,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "3D Node Scanner Engine",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Torch Button
            IconButton(
                onClick = {
                    val next = !isTorchOn
                    isTorchOn = next
                    camera?.cameraControl?.enableTorch(next)
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xCC121B14))
                    .border(1.dp, if (isTorchOn) NodeLime else Color(0x33FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Flash",
                    tint = if (isTorchOn) NodeLime else Color.White
                )
            }
        }

        // Bottom Controls: Instructions & Manual Barcode Input
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Align barcode or QR within the targeting frame",
                color = Color(0xFFE2E8F0),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onManualInputClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xDD121B14),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NodeGreen.copy(alpha = 0.5f)),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Manual Code",
                    tint = NodeLime,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enter Barcode Manually",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun triggerHaptic(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(80L, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(80L)
        }
    } catch (_: Exception) {}
}
