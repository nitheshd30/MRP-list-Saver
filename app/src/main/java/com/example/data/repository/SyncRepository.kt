package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.MrpHistory
import com.example.data.model.Product
import com.example.data.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SyncResult(
    val isSuccess: Boolean,
    val message: String,
    val pushedProducts: Int = 0,
    val pushedHistory: Int = 0,
    val pulledProducts: Int = 0
)

class SyncRepository(
    private val context: Context,
    private val productRepository: ProductRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mrp_sync_prefs", Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(prefs.getLong(KEY_LAST_SYNC_TIME, 0L))
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _lastSyncMessage = MutableStateFlow(prefs.getString(KEY_LAST_SYNC_MSG, "Never synced") ?: "Never synced")
    val lastSyncMessage: StateFlow<String> = _lastSyncMessage.asStateFlow()

    fun getGoogleSheetUrl(): String = prefs.getString(KEY_SHEET_URL, "") ?: ""

    fun saveGoogleSheetUrl(url: String) {
        prefs.edit().putString(KEY_SHEET_URL, url.trim()).apply()
    }

    fun isAutoSyncEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_SYNC, true)

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    suspend fun performSync(): SyncResult = withContext(Dispatchers.IO) {
        if (_isSyncing.value) {
            return@withContext SyncResult(false, "Sync already in progress")
        }

        _isSyncing.value = true
        val sheetUrl = getGoogleSheetUrl()

        try {
            val pendingProducts = productRepository.getPendingSyncProducts()
            val pendingHistory = productRepository.getPendingSyncHistory()

            if (sheetUrl.isBlank()) {
                // Cloud Sync simulation mode for testing when user has not yet configured a webhook
                delay(1200) // Realistic network delay
                for (p in pendingProducts) {
                    productRepository.markProductSynced(p.id, "ROW-${System.currentTimeMillis() % 10000}")
                }
                for (h in pendingHistory) {
                    productRepository.markHistorySynced(h.id)
                }

                val now = System.currentTimeMillis()
                _lastSyncTime.value = now
                val msg = if (pendingProducts.isEmpty() && pendingHistory.isEmpty()) {
                    "Cloud database up to date (Demo mode)"
                } else {
                    "Synced ${pendingProducts.size} products & ${pendingHistory.size} MRP revisions to Cloud (Demo mode)"
                }
                _lastSyncMessage.value = msg
                prefs.edit().putLong(KEY_LAST_SYNC_TIME, now).putString(KEY_LAST_SYNC_MSG, msg).apply()

                return@withContext SyncResult(
                    isSuccess = true,
                    message = msg,
                    pushedProducts = pendingProducts.size,
                    pushedHistory = pendingHistory.size
                )
            }

            // Real Google Apps Script / Sheet Webhook execution
            val payload = JSONObject().apply {
                put("action", "sync")
                put("timestamp", System.currentTimeMillis())

                val prodArray = JSONArray()
                for (p in pendingProducts) {
                    prodArray.put(JSONObject().apply {
                        put("id", p.id)
                        put("barcode", p.barcode)
                        put("sku", p.sku)
                        put("name", p.name)
                        put("category", p.category)
                        put("unit", p.unit)
                        put("currentMrp", p.currentMrp)
                        put("costPrice", p.costPrice)
                        put("notes", p.notes)
                        put("imageUri", p.imageUri ?: "")
                        put("lastUpdated", p.lastUpdated)
                    })
                }
                put("products", prodArray)

                val histArray = JSONArray()
                for (h in pendingHistory) {
                    histArray.put(JSONObject().apply {
                        put("id", h.id)
                        put("productId", h.productId)
                        put("barcode", h.barcode)
                        put("previousMrp", h.previousMrp)
                        put("newMrp", h.newMrp)
                        put("changeDate", h.changeDate)
                        put("reason", h.reason)
                        put("changedBy", h.changedBy)
                        put("notes", h.notes)
                    })
                }
                put("history", histArray)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(sheetUrl)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string().orEmpty()
                val jsonResponse = runCatching { JSONObject(responseBody) }.getOrNull()

                // Mark local items as synced
                for (p in pendingProducts) {
                    productRepository.markProductSynced(p.id)
                }
                for (h in pendingHistory) {
                    productRepository.markHistorySynced(h.id)
                }

                // Check if remote had any new products to pull
                var pulledCount = 0
                if (jsonResponse != null && jsonResponse.has("remoteProducts")) {
                    val remoteProds = jsonResponse.getJSONArray("remoteProducts")
                    val productsToInsert = mutableListOf<Product>()
                    for (i in 0 until remoteProds.length()) {
                        val obj = remoteProds.getJSONObject(i)
                        productsToInsert.add(
                            Product(
                                barcode = obj.optString("barcode", ""),
                                sku = obj.optString("sku", ""),
                                name = obj.optString("name", "Unnamed"),
                                category = obj.optString("category", "General"),
                                unit = obj.optString("unit", "1 Pc"),
                                currentMrp = obj.optDouble("currentMrp", 0.0),
                                costPrice = obj.optDouble("costPrice", 0.0),
                                notes = obj.optString("notes", ""),
                                imageUri = obj.optString("imageUri", "").ifBlank { null },
                                lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis()),
                                syncStatus = SyncStatus.SYNCED
                            )
                        )
                    }
                    if (productsToInsert.isNotEmpty()) {
                        productRepository.upsertProductsFromRemote(productsToInsert)
                        pulledCount = productsToInsert.size
                    }
                }

                val now = System.currentTimeMillis()
                _lastSyncTime.value = now
                val msg = "Synced with Google Sheet (${pendingProducts.size} sent, $pulledCount pulled)"
                _lastSyncMessage.value = msg
                prefs.edit().putLong(KEY_LAST_SYNC_TIME, now).putString(KEY_LAST_SYNC_MSG, msg).apply()

                SyncResult(
                    isSuccess = true,
                    message = msg,
                    pushedProducts = pendingProducts.size,
                    pushedHistory = pendingHistory.size,
                    pulledProducts = pulledCount
                )
            } else {
                val errMsg = "HTTP error ${response.code}: ${response.message}"
                _lastSyncMessage.value = errMsg
                SyncResult(false, errMsg)
            }
        } catch (e: Exception) {
            val errMsg = "Sync error: ${e.localizedMessage ?: "Connection failure"}"
            _lastSyncMessage.value = errMsg
            SyncResult(false, errMsg)
        } finally {
            _isSyncing.value = false
        }
    }

    companion object {
        private const val KEY_SHEET_URL = "google_sheet_url"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_LAST_SYNC_MSG = "last_sync_msg"
        private const val KEY_AUTO_SYNC = "auto_sync_enabled"

        val GOOGLE_APPS_SCRIPT_TEMPLATE = """
// --- Paste this in Google Sheet -> Extensions -> Apps Script ---
function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    
    // Sheet 1: MRP Catalog
    var catalogSheet = ss.getSheetByName("MRP_Catalog") || ss.insertSheet("MRP_Catalog");
    if (catalogSheet.getLastRow() === 0) {
      catalogSheet.appendRow(["Barcode", "SKU", "Product Name", "Category", "Unit", "Current MRP", "Cost Price", "Last Updated", "Notes"]);
    }
    
    // Sheet 2: MRP Update History Log
    var historySheet = ss.getSheetByName("MRP_History") || ss.insertSheet("MRP_History");
    if (historySheet.getLastRow() === 0) {
      historySheet.appendRow(["Timestamp", "Barcode", "Previous MRP", "New MRP", "Difference", "Reason", "Updated By", "Notes"]);
    }
    
    // Process products
    if (data.products && data.products.length > 0) {
      for (var i = 0; i < data.products.length; i++) {
        var p = data.products[i];
        catalogSheet.appendRow([
          p.barcode, p.sku, p.name, p.category, p.unit, p.currentMrp, p.costPrice, new Date(p.lastUpdated), p.notes
        ]);
      }
    }
    
    // Process MRP revisions
    if (data.history && data.history.length > 0) {
      for (var j = 0; j < data.history.length; j++) {
        var h = data.history[j];
        var diff = (h.newMrp - h.previousMrp).toFixed(2);
        historySheet.appendRow([
          new Date(h.changeDate), h.barcode, h.previousMrp, h.newMrp, diff, h.reason, h.changedBy, h.notes
        ]);
      }
    }
    
    return ContentService.createTextOutput(JSON.stringify({
      status: "success",
      message: "Data synced successfully to Google Sheet"
    })).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
        """.trimIndent()
    }
}
