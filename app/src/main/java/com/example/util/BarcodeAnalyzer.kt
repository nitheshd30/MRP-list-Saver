package com.example.util

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

class BarcodeAnalyzer(
    private val onBarcodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.QR_CODE
            ),
            DecodeHintType.TRY_HARDER to true
        )
        setHints(hints)
    }

    private var lastScannedCode = ""
    private var lastScannedTimestamp = 0L

    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        // Prevent flood: wait at least 1200ms before scanning the exact same barcode again
        val planes = imageProxy.planes
        if (planes.isNotEmpty()) {
            val buffer = planes[0].buffer
            val bytes = buffer.toByteArray()
            val width = imageProxy.width
            val height = imageProxy.height

            try {
                val source = PlanarYUVLuminanceSource(
                    bytes, width, height, 0, 0, width, height, false
                )
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val result = reader.decodeWithState(binaryBitmap)
                val code = result.text.trim()

                if (code.isNotBlank() && (code != lastScannedCode || (currentTime - lastScannedTimestamp > 1500))) {
                    lastScannedCode = code
                    lastScannedTimestamp = currentTime
                    onBarcodeScanned(code)
                }
            } catch (_: Exception) {
                // Not found in this frame, normal when aiming camera
            } finally {
                reader.reset()
            }
        }
        imageProxy.close()
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }
}
