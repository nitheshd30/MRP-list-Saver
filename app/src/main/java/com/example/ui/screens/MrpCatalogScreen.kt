package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MrpHistory
import com.example.data.model.Product
import com.example.data.model.SyncStatus
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.Card3D
import com.example.ui.components.FlipCard3D
import com.example.ui.components.Node3DAnimatedBanner
import com.example.ui.components.NodeFlipCard3D
import com.example.ui.screens.dialogs.AddCategoryDialog
import com.example.ui.screens.dialogs.AddEditProductDialog
import com.example.ui.screens.dialogs.UpdateMrpDialog
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.LaserCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NodeGreen
import com.example.ui.theme.NodeLime
import com.example.ui.viewmodel.MrpViewModel
import com.example.util.CsvExportManager
import com.example.util.PdfExportManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MrpCatalogScreen(
    viewModel: MrpViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.filteredProducts.collectAsState()
    val rawProducts by viewModel.rawProducts.collectAsState()
    val allHistory by viewModel.allHistory.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isManualOffline by viewModel.isManualOffline.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()

    // Flip state map for 3D flip card per product ID
    val flippedMap = remember { mutableStateMapOf<Long, Boolean>() }

    var productToUpdateMrp by remember { mutableStateOf<Product?>(null) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showSearchBarcodeScanner by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    if (showSearchBarcodeScanner) {
        BarcodeScannerDialog(
            title = "Scan Barcode to Search",
            onDismiss = { showSearchBarcodeScanner = false },
            onBarcodeScanned = { scannedCode ->
                viewModel.setSearchQuery(scannedCode)
                showSearchBarcodeScanner = false
            }
        )
    }

    Box(modifier = modifier.fillMaxSize().background(DarkCanvas)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Top App Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, LaserCyan, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = "App Icon",
                                    tint = LaserCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "MRP MANAGER",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Price Registry & History",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Offline toggle pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isOnline) Color(0x2610B981) else Color(0x26EF4444),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isOnline) Color(0x6610B981) else Color(0x66EF4444)
                            ),
                            modifier = Modifier.clickable { viewModel.toggleOfflineMode() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = "Connection Status",
                                    tint = if (isOnline) NeonEmerald else CoralRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isOnline) "Online" else if (isManualOffline) "Offline (Sim)" else "Offline",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnline) NeonEmerald else CoralRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Export dropdown menu button
                        Box {
                            IconButton(onClick = { showExportMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Export Options",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add New Category", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = LaserCyan) },
                                    onClick = {
                                        showExportMenu = false
                                        showAddCategoryDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export Products (CSV)", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, tint = LaserCyan) },
                                    onClick = {
                                        showExportMenu = false
                                        try {
                                            val file = CsvExportManager.exportProductsToCsv(context, rawProducts)
                                            CsvExportManager.shareCsvFile(context, file)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export Price History (CSV)", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = AmberGold) },
                                    onClick = {
                                        showExportMenu = false
                                        try {
                                            val file = CsvExportManager.exportHistoryToCsv(context, allHistory)
                                            CsvExportManager.shareCsvFile(context, file)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export Price Catalog (PDF)", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CoralRed) },
                                    onClick = {
                                        showExportMenu = false
                                        try {
                                            val file = PdfExportManager.exportPriceCatalogPdf(context, rawProducts)
                                            PdfExportManager.sharePdfFile(context, file)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3D Animated Node.js Type Interactive Banner
                    Node3DAnimatedBanner(
                        totalProducts = rawProducts.size,
                        totalCategories = categories.size.coerceAtLeast(1)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3D Stat Metrics Card
                    Card3D(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatPill(
                                label = "TOTAL ITEMS",
                                value = rawProducts.size.toString(),
                                accentColor = NodeLime
                            )
                            StatPill(
                                label = "CATEGORIES",
                                value = (categories.size.coerceAtLeast(1)).toString(),
                                accentColor = AmberGold
                            )
                            StatPill(
                                label = "PRICE LOGS",
                                value = allHistory.size.toString(),
                                accentColor = Color(0xFFA855F7)
                            )
                            StatPill(
                                label = "PENDING SYNC",
                                value = pendingSyncCount.toString(),
                                accentColor = if (pendingSyncCount > 0) CoralRed else NeonEmerald
                            )
                        }
                    }

                    // Offline status banner if offline
                    if (!isOnline) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x26EF4444),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44EF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = CoralRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Offline Mode Active: Price changes saved locally in Room DB and will sync once reconnected.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFCA5A5),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Search Field with Manual typing AND direct Barcode Scan button
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search by name, barcode, SKU...", color = Color(0xFF64748B), fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = LaserCyan)
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Direct Barcode Scanner Search Button
                                IconButton(
                                    onClick = { showSearchBarcodeScanner = true },
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan Barcode to Search",
                                        tint = LaserCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LaserCyan,
                            unfocusedBorderColor = DarkSurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Results Scope Selector: All Categories vs Added Categories
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "SEARCH RESULTS IN" else "FILTER BY CATEGORY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (searchQuery.isNotBlank()) AmberGold else Color(0xFF94A3B8),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Scope Toggle: "All Categories" vs "Added Categories"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selectedCategory == null) LaserCyan else Color.Transparent,
                                    modifier = Modifier.clickable { viewModel.setSelectedCategory(null) }
                                ) {
                                    Text(
                                        text = "All Categories",
                                        fontSize = 10.sp,
                                        fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedCategory == null) Color.Black else Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selectedCategory != null) LaserCyan else Color.Transparent,
                                    modifier = Modifier.clickable {
                                        if (selectedCategory == null && categories.isNotEmpty()) {
                                            viewModel.setSelectedCategory(categories.first())
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (selectedCategory != null) selectedCategory!! else "Added Categories ▾",
                                        fontSize = 10.sp,
                                        fontWeight = if (selectedCategory != null) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedCategory != null) Color.Black else Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Active Search Scope Banner
                    if (searchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33F59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = AmberGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${products.size} product${if (products.size != 1) "s" else ""} found for \"$searchQuery\"",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (selectedCategory == null) "Showing search results across all categories" else "Search results set to added category '$selectedCategory'",
                                        fontSize = 10.sp,
                                        color = if (selectedCategory == null) Color(0xFF94A3B8) else LaserCyan
                                    )
                                }

                                if (selectedCategory != null) {
                                    TextButton(
                                        onClick = { viewModel.setSelectedCategory(null) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Show in All",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberGold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Scrollable Chips with "+ Add Category" button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "+ Add Category" Button Chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, LaserCyan),
                            modifier = Modifier.clickable { showAddCategoryDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Category",
                                    tint = LaserCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ Category",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaserCyan
                                )
                            }
                        }

                        // "All Categories" Chip with count
                        val allMatchingCount = if (searchQuery.isBlank()) {
                            rawProducts.size
                        } else {
                            rawProducts.count { p ->
                                p.name.contains(searchQuery, ignoreCase = true) ||
                                p.barcode.contains(searchQuery, ignoreCase = true) ||
                                p.sku.contains(searchQuery, ignoreCase = true)
                            }
                        }

                        CategoryChip(
                            label = "All Categories",
                            count = allMatchingCount,
                            isSelected = selectedCategory == null,
                            onClick = { viewModel.setSelectedCategory(null) }
                        )

                        for (cat in categories) {
                            val catCount = if (searchQuery.isBlank()) {
                                rawProducts.count { it.category.equals(cat, ignoreCase = true) }
                            } else {
                                rawProducts.count { p ->
                                    p.category.equals(cat, ignoreCase = true) &&
                                    (p.name.contains(searchQuery, ignoreCase = true) ||
                                     p.barcode.contains(searchQuery, ignoreCase = true) ||
                                     p.sku.contains(searchQuery, ignoreCase = true))
                                }
                            }

                            CategoryChip(
                                label = cat,
                                count = catCount,
                                isSelected = selectedCategory == cat,
                                onClick = { viewModel.selectCategory(cat) }
                            )
                        }
                    }
                }
            }

            // Products List
            if (products.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No products match \"$searchQuery\"" else "No products found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Register products with barcodes and photos to manage MRP lists digitally.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = LaserCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Add First Product", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                items(products, key = { it.id }) { product ->
                    val isFlipped = flippedMap[product.id] ?: false
                    val productHistories = allHistory.filter { it.productId == product.id }

                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp)) {
                        NodeFlipCard3D(
                            isFlipped = isFlipped,
                            front = {
                                ProductFrontCard(
                                    product = product,
                                    historyCount = productHistories.size,
                                    onFlipToHistory = { flippedMap[product.id] = true },
                                    onUpdateMrp = { productToUpdateMrp = product },
                                    onEdit = { productToEdit = product },
                                    onDelete = { productToDelete = product }
                                )
                            },
                            back = {
                                ProductBackHistoryCard(
                                    product = product,
                                    historyList = productHistories,
                                    onFlipBack = { flippedMap[product.id] = false },
                                    onUpdateMrp = { productToUpdateMrp = product }
                                )
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Register New Product
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = AmberGold,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Product", modifier = Modifier.size(28.dp))
        }
    }

    // Dialogs
    if (productToUpdateMrp != null) {
        UpdateMrpDialog(
            product = productToUpdateMrp!!,
            onDismiss = { productToUpdateMrp = null },
            onConfirm = { newMrp, reason, changedBy, notes ->
                viewModel.updateMrp(productToUpdateMrp!!, newMrp, reason, changedBy, notes)
                productToUpdateMrp = null
            }
        )
    }

    if (showAddDialog) {
        AddEditProductDialog(
            availableCategories = categories,
            onAddCategory = { newCat ->
                viewModel.addCategory(newCat)
            },
            onDismiss = { showAddDialog = false },
            onConfirm = { newProduct ->
                viewModel.saveProduct(newProduct)
                Toast.makeText(context, "Product saved! Syncing with Google Sheet...", Toast.LENGTH_SHORT).show()
                showAddDialog = false
            }
        )
    }

    if (productToEdit != null) {
        AddEditProductDialog(
            productToEdit = productToEdit,
            availableCategories = categories,
            onAddCategory = { newCat ->
                viewModel.addCategory(newCat)
            },
            onDismiss = { productToEdit = null },
            onConfirm = { updated ->
                viewModel.saveProduct(updated)
                Toast.makeText(context, "Product updated! Syncing with Google Sheet...", Toast.LENGTH_SHORT).show()
                productToEdit = null
            }
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            existingCategories = categories,
            onDismiss = { showAddCategoryDialog = false },
            onCategoryAdded = { newCat ->
                val added = viewModel.addCategory(newCat)
                if (added) {
                    Toast.makeText(context, "Category '$newCat' created successfully!", Toast.LENGTH_SHORT).show()
                }
                showAddCategoryDialog = false
            }
        )
    }

    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product?", color = Color.White) },
            text = {
                Text(
                    "Are you sure you want to delete \"${productToDelete?.name}\"? All MRP historical price logs for this barcode will also be removed.",
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        productToDelete?.let { viewModel.deleteProduct(it) }
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    accentColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = accentColor
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun CategoryChip(
    label: String,
    count: Int? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AmberGold else Color(0xFF1E293B),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) AmberGold else Color(0x33FFFFFF)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.Black else Color(0xFFCBD5E1)
            )
            if (count != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color.Black.copy(alpha = 0.2f) else Color(0xFF0F172A)
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else LaserCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductFrontCard(
    product: Product,
    historyCount: Int,
    onFlipToHistory: () -> Unit,
    onUpdateMrp: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0x2606B6D4),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = product.category.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = LaserCyan,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (product.sku.isNotBlank()) {
                Text(
                    text = "SKU: ${product.sku}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Sync status icon
            Icon(
                imageVector = if (product.syncStatus == SyncStatus.SYNCED) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                contentDescription = product.syncStatus.name,
                tint = if (product.syncStatus == SyncStatus.SYNCED) NeonEmerald else AmberGold,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Quick direct Update MRP icon
            IconButton(onClick = onUpdateMrp, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.PriceChange,
                    contentDescription = "Update MRP",
                    tint = AmberGold,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Product Main Details: Image + Title/Barcode
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!product.imageUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0x3306B6D4), RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = File(product.imageUri).takeIf { it.exists() } ?: product.imageUri,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Pack: ${product.unit}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "Barcode: ${product.barcode}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = LaserCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Big 3D MRP Price Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33F59E0B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MAXIMUM RETAIL PRICE (MRP)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "₹ ${String.format(Locale.US, "%.2f", product.currentMrp)}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = AmberGold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Direct Quick Update MRP button inside price badge
                Button(
                    onClick = onUpdateMrp,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGold,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PriceChange,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Update MRP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Last Revised",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = dateFormat.format(Date(product.lastUpdated)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons: Flip to History & Quick Update MRP
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onFlipToHistory,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4D06B6D4)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = LaserCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Price History ($historyCount)", color = LaserCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onUpdateMrp,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.PriceChange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Update MRP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProductBackHistoryCard(
    product: Product,
    historyList: List<MrpHistory>,
    onFlipBack: () -> Unit,
    onUpdateMrp: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("dd MMM yy • HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = AmberGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "MRP UPDATE HISTORY",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberGold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = product.name,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                color = Color(0x33FFFFFF),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { onFlipBack() }
            ) {
                Text(
                    text = "Flip to Front",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (historyList.isEmpty()) {
            Text(
                text = "No prior price revisions recorded yet.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (h in historyList.take(3)) {
                    val diff = h.newMrp - h.previousMrp
                    val isPriceHike = diff > 0

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "₹ ${String.format(Locale.US, "%.2f", h.previousMrp)} → ₹ ${String.format(Locale.US, "%.2f", h.newMrp)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (diff != 0.0) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isPriceHike) Color(0x33EF4444) else Color(0x3310B981)
                                        ) {
                                            Text(
                                                text = String.format(Locale.US, "%+.2f", diff),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPriceHike) CoralRed else NeonEmerald,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${h.reason} • by ${h.changedBy}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Text(
                                text = timeFormat.format(Date(h.changeDate)),
                                fontSize = 9.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onUpdateMrp,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PriceChange, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Record New MRP Change", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
