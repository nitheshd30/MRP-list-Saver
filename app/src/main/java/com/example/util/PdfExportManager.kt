package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.model.MrpHistory
import com.example.data.model.Product
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportManager {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

    fun exportPrintableLabelsPdf(context: Context, products: List<Product>): File {
        val document = PdfDocument()
        val exportDir = File(context.cacheDir, "mrp_exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val file = File(exportDir, "MRP_Printable_Labels_$timestamp.pdf")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
        }

        val labelsPerPage = 8 // 2 columns x 4 rows
        val colWidth = 270f
        val rowHeight = 180f
        val marginX = 25f
        val marginY = 40f

        val totalPages = (products.size + labelsPerPage - 1) / labelsPerPage
        var currentProductIndex = 0

        for (pageIndex in 0 until (if (totalPages == 0) 1 else totalPages)) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Page Header
            paint.color = Color.rgb(30, 41, 59)
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("LABEL PRINTING INCHARGE - PRODUCT MRP TAGS", marginX, 25f, paint)

            paint.color = Color.GRAY
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Generated: ${dateFormat.format(Date())}  |  Page ${pageIndex + 1} of $totalPages", PAGE_WIDTH - marginX - 180f, 25f, paint)

            // Draw labels
            for (row in 0 until 4) {
                for (col in 0 until 2) {
                    if (currentProductIndex >= products.size) break

                    val p = products[currentProductIndex]
                    val left = marginX + (col * (colWidth + 10f))
                    val top = marginY + (row * (rowHeight + 15f))
                    val right = left + colWidth
                    val bottom = top + rowHeight

                    drawSingleLabelTag(canvas, p, left, top, right, bottom, paint, titlePaint)
                    currentProductIndex++
                }
            }

            document.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    private fun drawSingleLabelTag(
        canvas: Canvas,
        product: Product,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint,
        titlePaint: Paint
    ) {
        val tagWidth = right - left

        // Tag background
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(left, top, right, bottom), 6f, 6f, paint)

        // Dashed border for cutting
        paint.color = Color.rgb(180, 180, 180)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        canvas.drawRoundRect(RectF(left, top, right, bottom), 6f, 6f, paint)
        paint.pathEffect = null // reset

        // Header color strip
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(15, 23, 42) // Dark Navy
        canvas.drawRoundRect(RectF(left + 1, top + 1, right - 1, top + 26f), 4f, 4f, paint)

        // Category Tag
        paint.color = Color.rgb(251, 191, 36) // Amber
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(product.category.uppercase(), left + 10f, top + 17f, paint)

        // SKU on right of header
        paint.color = Color.WHITE
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        val skuText = if (product.sku.isNotBlank()) "SKU: ${product.sku}" else product.unit
        canvas.drawText(skuText, right - 10f - paint.measureText(skuText), top + 17f, paint)

        // Product Name (truncated if too long)
        titlePaint.textSize = 12f
        titlePaint.color = Color.BLACK
        var nameToDraw = product.name
        if (titlePaint.measureText(nameToDraw) > (tagWidth - 20f)) {
            while (nameToDraw.length > 3 && titlePaint.measureText("$nameToDraw...") > (tagWidth - 20f)) {
                nameToDraw = nameToDraw.dropLast(1)
            }
            nameToDraw = "$nameToDraw..."
        }
        canvas.drawText(nameToDraw, left + 10f, top + 45f, titlePaint)

        // Unit / Pack info
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Net Qty: ${product.unit}", left + 10f, top + 60f, paint)

        // Big MRP Box
        paint.color = Color.rgb(241, 245, 249) // Light grey badge
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(left + 10f, top + 68f, right - 10f, top + 110f), 6f, 6f, paint)

        paint.color = Color.rgb(220, 38, 38) // Bold Red/Burgundy label
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("MAXIMUM RETAIL PRICE (M.R.P.):", left + 18f, top + 83f, paint)

        // Big Price Currency
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val priceText = "₹ ${String.format(Locale.US, "%.2f", product.currentMrp)}"
        canvas.drawText(priceText, left + 18f, top + 104f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("(Incl. of all taxes)", left + 24f + paint.measureText(priceText) + 10f, top + 102f, paint)

        // Barcode Image
        val barcodeBitmap = BarcodeGenerator.generateBarcodeBitmap(
            product.barcode,
            width = 320,
            height = 65
        )
        if (barcodeBitmap != null) {
            val bcDst = RectF(left + 25f, top + 116f, right - 25f, top + 155f)
            canvas.drawBitmap(barcodeBitmap, null, bcDst, null)
        }

        // Barcode numbers below barcode
        paint.color = Color.BLACK
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        val bcTextWidth = paint.measureText(product.barcode)
        canvas.drawText(product.barcode, left + (tagWidth - bcTextWidth) / 2f, top + 168f, paint)

        // Print Date note
        paint.color = Color.GRAY
        paint.textSize = 6.5f
        paint.typeface = Typeface.DEFAULT
        val printDateStr = "PRINT: ${dateFormat.format(Date(product.lastUpdated))}"
        canvas.drawText(printDateStr, right - 10f - paint.measureText(printDateStr), top + 175f, paint)
    }

    fun exportPriceCatalogPdf(context: Context, products: List<Product>): File {
        val document = PdfDocument()
        val exportDir = File(context.cacheDir, "mrp_exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val file = File(exportDir, "MRP_Master_Catalog_$timestamp.pdf")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rowsPerPage = 22
        val totalPages = (products.size + rowsPerPage - 1) / rowsPerPage
        var prodIdx = 0

        for (pageIdx in 0 until (if (totalPages == 0) 1 else totalPages)) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIdx + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Header Banner
            paint.color = Color.rgb(15, 23, 42)
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 60f, paint)

            paint.color = Color.WHITE
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("MASTER PRODUCT MRP CATALOG", 25f, 32f, paint)

            paint.color = Color.rgb(148, 163, 184)
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Official Label & Pricing Records | Page ${pageIdx + 1} of $totalPages", 25f, 48f, paint)

            val dateStr = "Date: ${dateFormat.format(Date())}"
            canvas.drawText(dateStr, PAGE_WIDTH - 25f - paint.measureText(dateStr), 35f, paint)

            // Table Columns Header
            var y = 85f
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRect(20f, y - 14f, PAGE_WIDTH - 20f, y + 10f, paint)

            paint.color = Color.rgb(51, 65, 85)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            canvas.drawText("BARCODE", 25f, y, paint)
            canvas.drawText("PRODUCT NAME", 130f, y, paint)
            canvas.drawText("CATEGORY", 320f, y, paint)
            canvas.drawText("UNIT", 400f, y, paint)
            canvas.drawText("MRP (₹)", 455f, y, paint)
            canvas.drawText("UPDATED", 515f, y, paint)

            y += 24f
            paint.typeface = Typeface.DEFAULT

            for (r in 0 until rowsPerPage) {
                if (prodIdx >= products.size) break
                val p = products[prodIdx]

                // Alternating row background
                if (r % 2 == 1) {
                    paint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(20f, y - 12f, PAGE_WIDTH - 20f, y + 14f, paint)
                }

                paint.color = Color.rgb(30, 41, 59)
                paint.textSize = 8.5f
                canvas.drawText(p.barcode, 25f, y, paint)

                var name = p.name
                if (name.length > 28) name = name.take(25) + "..."
                canvas.drawText(name, 130f, y, paint)

                canvas.drawText(p.category, 320f, y, paint)
                canvas.drawText(p.unit, 400f, y, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.rgb(220, 38, 38)
                canvas.drawText(String.format(Locale.US, "%.2f", p.currentMrp), 455f, y, paint)

                paint.typeface = Typeface.DEFAULT
                paint.color = Color.GRAY
                paint.textSize = 7.5f
                canvas.drawText(dateFormat.format(Date(p.lastUpdated)), 515f, y, paint)

                y += 24f
                prodIdx++
            }

            document.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    fun exportMrpHistoryPdf(context: Context, historyList: List<MrpHistory>): File {
        val document = PdfDocument()
        val exportDir = File(context.cacheDir, "mrp_exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val file = File(exportDir, "MRP_Price_Revision_Audit_$timestamp.pdf")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rowsPerPage = 20
        val totalPages = (historyList.size + rowsPerPage - 1) / rowsPerPage
        var histIdx = 0

        for (pageIdx in 0 until (if (totalPages == 0) 1 else totalPages)) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIdx + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Header Banner
            paint.color = Color.rgb(30, 27, 75) // Dark Indigo
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 60f, paint)

            paint.color = Color.WHITE
            paint.textSize = 15f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("MRP PRICE REVISION AUDIT REPORT", 25f, 30f, paint)

            paint.color = Color.rgb(199, 210, 254)
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Historical Price Tracking & Label Change Records | Page ${pageIdx + 1} of $totalPages", 25f, 46f, paint)

            // Table Header
            var y = 85f
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRect(20f, y - 14f, PAGE_WIDTH - 20f, y + 10f, paint)

            paint.color = Color.rgb(51, 65, 85)
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            canvas.drawText("DATE & TIME", 25f, y, paint)
            canvas.drawText("BARCODE", 110f, y, paint)
            canvas.drawText("OLD MRP", 200f, y, paint)
            canvas.drawText("NEW MRP", 255f, y, paint)
            canvas.drawText("DIFF", 315f, y, paint)
            canvas.drawText("REASON", 365f, y, paint)
            canvas.drawText("UPDATED BY", 475f, y, paint)

            y += 24f
            paint.typeface = Typeface.DEFAULT

            for (r in 0 until rowsPerPage) {
                if (histIdx >= historyList.size) break
                val h = historyList[histIdx]

                if (r % 2 == 1) {
                    paint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(20f, y - 12f, PAGE_WIDTH - 20f, y + 14f, paint)
                }

                paint.color = Color.GRAY
                paint.textSize = 7.5f
                canvas.drawText(timeFormat.format(Date(h.changeDate)), 25f, y, paint)

                paint.color = Color.BLACK
                paint.textSize = 8f
                canvas.drawText(h.barcode, 110f, y, paint)

                paint.color = Color.rgb(100, 116, 139)
                canvas.drawText(String.format(Locale.US, "%.2f", h.previousMrp), 200f, y, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.rgb(15, 23, 42)
                canvas.drawText(String.format(Locale.US, "%.2f", h.newMrp), 255f, y, paint)

                val diff = h.newMrp - h.previousMrp
                paint.color = if (diff >= 0) Color.rgb(220, 38, 38) else Color.rgb(16, 185, 129)
                canvas.drawText(String.format(Locale.US, "%+.2f", diff), 315f, y, paint)

                paint.typeface = Typeface.DEFAULT
                paint.color = Color.rgb(30, 41, 59)
                var reason = h.reason
                if (reason.length > 20) reason = reason.take(18) + "..."
                canvas.drawText(reason, 365f, y, paint)

                var by = h.changedBy
                if (by.length > 18) by = by.take(16) + "..."
                canvas.drawText(by, 475f, y, paint)

                y += 24f
                histIdx++
            }

            document.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    fun sharePdfFile(context: Context, file: File, title: String = "Share MRP PDF") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            putExtra(Intent.EXTRA_TEXT, "Exported PDF document from MRP Manager.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun printPdfFile(context: Context, file: File) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager != null) {
            val printAdapter = object : android.print.PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = android.print.PrintDocumentInfo.Builder(file.name)
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        val input = file.inputStream()
                        val output = FileOutputStream(destination?.fileDescriptor)
                        input.copyTo(output)
                        input.close()
                        output.close()
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.localizedMessage)
                    }
                }
            }
            printManager.print("MRP_Print_${file.nameWithoutExtension}", printAdapter, PrintAttributes.Builder().build())
        } else {
            sharePdfFile(context, file, "Print Document via...")
        }
    }
}
