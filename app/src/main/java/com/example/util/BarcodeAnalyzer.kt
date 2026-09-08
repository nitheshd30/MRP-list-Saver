package com.example.util

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

/**
 * High-performance, low-latency barcode analyzer powered by Google ML Kit.
 * Specifically configured for instantaneous detection of EAN-13, EAN-8, UPC-A, UPC-E,
 * Code-128, Code-39, and QR Codes, with full rotation awareness for portrait camera orientations.
 */
class BarcodeAnalyzer(
    private val onBarcodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // Configure ML Kit specifically for retail product barcodes
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_QR_CODE
        )
        .build()

    private val mlKitScanner = BarcodeScanning.getClient(options)

    private val zxingReader = MultiFormatReader().apply {
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

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        val mediaImage = imageProxy.image

        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            mlKitScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    var detected = false
                    for (barcode in barcodes) {
                        val raw = barcode.rawValue?.trim() ?: barcode.displayValue?.trim()
                        if (!raw.isNullOrBlank()) {
                            if (raw != lastScannedCode || (currentTime - lastScannedTimestamp > 1500)) {
                                lastScannedCode = raw
                                lastScannedTimestamp = currentTime
                                onBarcodeScanned(raw)
                            }
                            detected = true
                            break
                        }
                    }

                    if (!detected) {
                        // If ML Kit didn't detect on this specific frame, try ZXing fallback
                        decodeWithZxing(imageProxy, currentTime)
                    }
                }
                .addOnFailureListener {
                    decodeWithZxing(imageProxy, currentTime)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            decodeWithZxing(imageProxy, currentTime)
            imageProxy.close()
        }
    }

    private fun decodeWithZxing(imageProxy: ImageProxy, currentTime: Long) {
        try {
            val planes = imageProxy.planes
            if (planes.isNotEmpty()) {
                val buffer = planes[0].buffer
                val bytes = buffer.toByteArray()
                val width = imageProxy.width
                val height = imageProxy.height

                val source = PlanarYUVLuminanceSource(
                    bytes, width, height, 0, 0, width, height, false
                )
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val result = zxingReader.decodeWithState(binaryBitmap)
                val code = result.text.trim()

                if (code.isNotBlank() && (code != lastScannedCode || (currentTime - lastScannedTimestamp > 1500))) {
                    lastScannedCode = code
                    lastScannedTimestamp = currentTime
                    onBarcodeScanned(code)
                }
            }
        } catch (_: Exception) {
            // Frame did not contain a readable barcode
        } finally {
            zxingReader.reset()
        }
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }
}
