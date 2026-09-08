package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.MrpHistory
import com.example.data.model.Product
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun exportProductsToCsv(context: Context, products: List<Product>): File {
        val exportDir = File(context.cacheDir, "mrp_exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val file = File(exportDir, "MRP_Catalog_$timestamp.csv")

        val sb = StringBuilder()
        // Header
        sb.append("Barcode,SKU,Product Name,Category,Unit,Current MRP,Cost Price,Last Updated,Sync Status,Notes\n")

        for (p in products) {
            val dateStr = dateFormat.format(Date(p.lastUpdated))
            sb.append(escapeCsv(p.barcode)).append(",")
            sb.append(escapeCsv(p.sku)).append(",")
            sb.append(escapeCsv(p.name)).append(",")
            sb.append(escapeCsv(p.category)).append(",")
            sb.append(escapeCsv(p.unit)).append(",")
            sb.append(String.format(Locale.US, "%.2f", p.currentMrp)).append(",")
            sb.append(String.format(Locale.US, "%.2f", p.costPrice)).append(",")
            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(escapeCsv(p.syncStatus.name)).append(",")
            sb.append(escapeCsv(p.notes)).append("\n")
        }

        file.writeText(sb.toString())
        return file
    }

    fun exportHistoryToCsv(context: Context, historyList: List<MrpHistory>): File {
        val exportDir = File(context.cacheDir, "mrp_exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val file = File(exportDir, "MRP_History_Audit_$timestamp.csv")

        val sb = StringBuilder()
        // Header
        sb.append("Date & Time,Barcode,Previous MRP,New MRP,Difference,Reason,Updated By,Notes\n")

        for (h in historyList) {
            val dateStr = dateFormat.format(Date(h.changeDate))
            val diff = h.newMrp - h.previousMrp
            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(escapeCsv(h.barcode)).append(",")
            sb.append(String.format(Locale.US, "%.2f", h.previousMrp)).append(",")
            sb.append(String.format(Locale.US, "%.2f", h.newMrp)).append(",")
            sb.append(String.format(Locale.US, "%+.2f", diff)).append(",")
            sb.append(escapeCsv(h.reason)).append(",")
            sb.append(escapeCsv(h.changedBy)).append(",")
            sb.append(escapeCsv(h.notes)).append("\n")
        }

        file.writeText(sb.toString())
        return file
    }

    fun shareCsvFile(context: Context, file: File, title: String = "Share MRP Export") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            putExtra(Intent.EXTRA_TEXT, "Exported MRP data from MRP Manager App.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
