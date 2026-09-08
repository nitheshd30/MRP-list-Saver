package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.util.EnumMap

object BarcodeGenerator {
    fun generateBarcodeBitmap(
        content: String,
        width: Int = 400,
        height: Int = 160,
        format: BarcodeFormat = BarcodeFormat.CODE_128
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.MARGIN, 1)
            }
            val bitMatrix = MultiFormatWriter().encode(content, format, width, height, hints)
            val bmWidth = bitMatrix.width
            val bmHeight = bitMatrix.height
            val pixels = IntArray(bmWidth * bmHeight)

            for (y in 0 until bmHeight) {
                val offset = y * bmWidth
                for (x in 0 until bmWidth) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }

            val bitmap = Bitmap.createBitmap(bmWidth, bmHeight, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, bmWidth, 0, 0, bmWidth, bmHeight)
            bitmap
        } catch (e: Exception) {
            // Fallback to QR code if code_128 can't encode or formatting error
            try {
                val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height)
                val bmWidth = bitMatrix.width
                val bmHeight = bitMatrix.height
                val pixels = IntArray(bmWidth * bmHeight)
                for (y in 0 until bmHeight) {
                    val offset = y * bmWidth
                    for (x in 0 until bmWidth) {
                        pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                    }
                }
                val bitmap = Bitmap.createBitmap(bmWidth, bmHeight, Bitmap.Config.ARGB_8888)
                bitmap.setPixels(pixels, 0, bmWidth, 0, 0, bmWidth, bmHeight)
                bitmap
            } catch (e2: Exception) {
                null
            }
        }
    }
}
