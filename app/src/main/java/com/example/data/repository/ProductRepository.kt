package com.example.data.repository

import com.example.data.local.MrpHistoryDao
import com.example.data.local.ProductDao
import com.example.data.model.MrpHistory
import com.example.data.model.Product
import com.example.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao,
    private val mrpHistoryDao: MrpHistoryDao
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allCategories: Flow<List<String>> = productDao.getCategories()
    val allHistory: Flow<List<MrpHistory>> = mrpHistoryDao.getAllHistory()

    fun searchProducts(query: String): Flow<List<Product>> {
        return if (query.isBlank()) {
            productDao.getAllProducts()
        } else {
            productDao.searchProducts(query.trim())
        }
    }

    suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.getProductByBarcode(barcode.trim())
    }

    fun getHistoryForProduct(productId: Long): Flow<List<MrpHistory>> {
        return mrpHistoryDao.getHistoryForProduct(productId)
    }

    fun getHistoryForBarcode(barcode: String): Flow<List<MrpHistory>> {
        return mrpHistoryDao.getHistoryForBarcode(barcode.trim())
    }

    suspend fun saveProduct(product: Product, isOffline: Boolean = false): Long {
        val syncStatus = if (product.id != 0L) SyncStatus.PENDING_UPDATE else SyncStatus.PENDING_INSERT
        val productToSave = product.copy(
            syncStatus = syncStatus,
            lastUpdated = System.currentTimeMillis()
        )
        val id = productDao.insert(productToSave)

        // Record initial MRP in history
        mrpHistoryDao.insert(
            MrpHistory(
                productId = if (product.id != 0L) product.id else id,
                barcode = product.barcode,
                previousMrp = product.currentMrp,
                newMrp = product.currentMrp,
                changeDate = System.currentTimeMillis(),
                reason = "Initial Product Registration",
                changedBy = "Label Printing Incharge",
                notes = "Registered into digital MRP database",
                syncStatus = syncStatus
            )
        )
        return id
    }

    suspend fun updateMrp(
        product: Product,
        newMrp: Double,
        reason: String,
        changedBy: String,
        notes: String,
        isOffline: Boolean = false
    ) {
        val previousMrp = product.currentMrp
        val syncStatus = SyncStatus.PENDING_UPDATE
        val updatedProduct = product.copy(
            currentMrp = newMrp,
            lastUpdated = System.currentTimeMillis(),
            syncStatus = syncStatus
        )
        productDao.update(updatedProduct)

        // Record historical price revision
        mrpHistoryDao.insert(
            MrpHistory(
                productId = product.id,
                barcode = product.barcode,
                previousMrp = previousMrp,
                newMrp = newMrp,
                changeDate = System.currentTimeMillis(),
                reason = reason.ifBlank { "Price Revision" },
                changedBy = changedBy.ifBlank { "Label Incharge" },
                notes = notes,
                syncStatus = syncStatus
            )
        )
    }

    suspend fun deleteProduct(product: Product) {
        productDao.delete(product)
    }

    suspend fun getPendingSyncProducts(): List<Product> = productDao.getPendingSyncProducts()
    suspend fun getPendingSyncHistory(): List<MrpHistory> = mrpHistoryDao.getPendingSyncHistory()

    suspend fun markProductSynced(id: Long, sheetRowId: String = "") {
        productDao.updateSyncStatus(id, SyncStatus.SYNCED, sheetRowId)
    }

    suspend fun markHistorySynced(id: Long) {
        mrpHistoryDao.updateSyncStatus(id, SyncStatus.SYNCED)
    }

    suspend fun upsertProductsFromRemote(products: List<Product>) {
        productDao.insertAll(products)
    }
}
