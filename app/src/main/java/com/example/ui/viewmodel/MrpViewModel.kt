package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.MrpHistory
import com.example.data.model.Product
import com.example.data.repository.CategoryRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.SyncRepository
import com.example.data.repository.SyncResult
import com.example.util.CsvExportManager
import com.example.util.NetworkMonitor
import com.example.util.PdfExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MrpViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val productRepository = ProductRepository(database.productDao(), database.mrpHistoryDao())
    val categoryRepository = CategoryRepository(application, database.productDao())
    val syncRepository = SyncRepository(application, productRepository)
    val networkMonitor = NetworkMonitor(application)

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val isManualOffline: StateFlow<Boolean> = networkMonitor.isManualOffline

    val isSyncing: StateFlow<Boolean> = syncRepository.isSyncing
    val lastSyncTime: StateFlow<Long> = syncRepository.lastSyncTime
    val lastSyncMessage: StateFlow<String> = syncRepository.lastSyncMessage

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val allCategories: StateFlow<List<String>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawProducts: StateFlow<List<Product>> = productRepository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHistory: StateFlow<List<MrpHistory>> = productRepository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProducts: StateFlow<List<Product>> = combine(
        rawProducts,
        _searchQuery,
        _selectedCategory
    ) { prods, query, category ->
        prods.filter { p ->
            val matchesQuery = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.barcode.contains(query, ignoreCase = true) ||
                    p.sku.contains(query, ignoreCase = true)
            val matchesCat = category == null || p.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = rawProducts.combine(allHistory) { prods, hist ->
        val pendingProds = prods.count { it.syncStatus != com.example.data.model.SyncStatus.SYNCED }
        val pendingHist = hist.count { it.syncStatus != com.example.data.model.SyncStatus.SYNCED }
        pendingProds + pendingHist
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Scanner state
    private val _scannedProduct = MutableStateFlow<Product?>(null)
    val scannedProduct: StateFlow<Product?> = _scannedProduct.asStateFlow()

    private val _scannedBarcode = MutableStateFlow<String?>(null)
    val scannedBarcode: StateFlow<String?> = _scannedBarcode.asStateFlow()

    private val _productHistory = MutableStateFlow<List<MrpHistory>>(emptyList())
    val productHistory: StateFlow<List<MrpHistory>> = _productHistory.asStateFlow()

    // Active bottom navigation tab: 0=Catalog, 1=Scanner, 2=Label Print, 3=Google Sheets Sync
    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun addCategory(name: String): Boolean {
        val success = categoryRepository.addCategory(name)
        if (success) {
            _selectedCategory.value = name.trim()
        }
        return success
    }

    fun removeCategory(name: String) {
        categoryRepository.removeCategory(name)
        if (_selectedCategory.value == name) {
            _selectedCategory.value = null
        }
    }

    fun onBarcodeScanned(barcode: String) {
        onBarcodeDetected(barcode)
    }

    fun toggleOfflineMode() {
        networkMonitor.toggleManualOfflineMode()
    }

    fun onBarcodeDetected(barcode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _scannedBarcode.value = barcode
            val found = productRepository.getProductByBarcode(barcode)
            _scannedProduct.value = found
            if (found != null) {
                loadHistoryForProduct(found.id)
            } else {
                _productHistory.value = emptyList()
            }
        }
    }

    fun clearScannedResult() {
        _scannedProduct.value = null
        _scannedBarcode.value = null
        _productHistory.value = emptyList()
    }

    fun loadHistoryForProduct(productId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.getHistoryForProduct(productId).collect {
                _productHistory.value = it
            }
        }
    }

    fun saveProduct(
        product: Product,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isOffline = !isOnline.value
                productRepository.saveProduct(product, isOffline)
                if (isOnline.value && syncRepository.isAutoSyncEnabled()) {
                    syncRepository.performSync()
                }
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun updateMrp(
        product: Product,
        newMrp: Double,
        reason: String,
        changedBy: String,
        notes: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isOffline = !isOnline.value
                productRepository.updateMrp(product, newMrp, reason, changedBy, notes, isOffline)
                if (isOnline.value && syncRepository.isAutoSyncEnabled()) {
                    syncRepository.performSync()
                }
                // Refresh scanned product if it's currently open
                if (_scannedProduct.value?.id == product.id) {
                    _scannedProduct.value = product.copy(currentMrp = newMrp)
                    loadHistoryForProduct(product.id)
                }
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.deleteProduct(product)
        }
    }

    fun triggerSync(onResult: (SyncResult) -> Unit = {}) {
        viewModelScope.launch {
            val result = syncRepository.performSync()
            onResult(result)
        }
    }

    fun saveGoogleSheetUrl(url: String) {
        syncRepository.saveGoogleSheetUrl(url)
    }

    fun testGoogleSheetConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = syncRepository.testConnection()
            onResult(res.first, res.second)
        }
    }

    // Export helpers
    fun exportProductsToCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val prods = rawProducts.value
            val file = CsvExportManager.exportProductsToCsv(context, prods)
            CsvExportManager.shareCsvFile(context, file, "Share Product MRP CSV")
        }
    }

    fun exportHistoryToCsv(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val hist = allHistory.value
            val file = CsvExportManager.exportHistoryToCsv(context, hist)
            CsvExportManager.shareCsvFile(context, file, "Share MRP History CSV")
        }
    }

    fun exportPrintableLabelsPdf(context: Context, productsToPrint: List<Product>? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val prods = productsToPrint ?: rawProducts.value
            val file = PdfExportManager.exportPrintableLabelsPdf(context, prods)
            PdfExportManager.printPdfFile(context, file)
        }
    }

    fun sharePrintableLabelsPdf(context: Context, productsToPrint: List<Product>? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val prods = productsToPrint ?: rawProducts.value
            val file = PdfExportManager.exportPrintableLabelsPdf(context, prods)
            PdfExportManager.sharePdfFile(context, file, "Share Printable Labels PDF")
        }
    }

    fun exportPriceCatalogPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val prods = rawProducts.value
            val file = PdfExportManager.exportPriceCatalogPdf(context, prods)
            PdfExportManager.sharePdfFile(context, file, "Share Master MRP Catalog PDF")
        }
    }

    fun exportMrpHistoryPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val hist = allHistory.value
            val file = PdfExportManager.exportMrpHistoryPdf(context, hist)
            PdfExportManager.sharePdfFile(context, file, "Share Price Revision Audit PDF")
        }
    }
}
