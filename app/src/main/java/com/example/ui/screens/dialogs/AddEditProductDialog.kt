package com.example.ui.screens.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.Product
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.LaserCyan
import com.example.util.ImageStorageManager
import java.io.File
import java.util.Locale

@Composable
fun AddEditProductDialog(
    initialBarcode: String = "",
    productToEdit: Product? = null,
    availableCategories: List<String> = emptyList(),
    onAddCategory: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit
) {
    val context = LocalContext.current

    var barcode by remember { mutableStateOf(productToEdit?.barcode ?: initialBarcode) }
    var sku by remember { mutableStateOf(productToEdit?.sku ?: "") }
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var category by remember { mutableStateOf(productToEdit?.category ?: "") }
    var unit by remember { mutableStateOf(productToEdit?.unit ?: "1 Pc") }
    var mrpText by remember {
        mutableStateOf(if (productToEdit != null) String.format(Locale.US, "%.2f", productToEdit.currentMrp) else "")
    }
    var costPriceText by remember {
        mutableStateOf(if (productToEdit != null && productToEdit.costPrice > 0) String.format(Locale.US, "%.2f", productToEdit.costPrice) else "")
    }
    var notes by remember { mutableStateOf(productToEdit?.notes ?: "") }
    var imageUriString by remember { mutableStateOf(productToEdit?.imageUri) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val categoriesList = remember(availableCategories, category) {
        val set = linkedSetOf<String>()
        if (category.isNotBlank()) set.add(category)
        set.addAll(availableCategories)
        if (set.isEmpty()) {
            set.addAll(listOf("Grocery", "Dairy", "Beverages", "Snacks", "Bakery", "Personal Care", "Household", "General"))
        }
        set.toList()
    }

    // Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageManager.saveImageToInternalStorage(context, uri)
            if (savedPath != null) {
                imageUriString = savedPath
            }
        }
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            title = "Scan Product Barcode",
            onDismiss = { showBarcodeScanner = false },
            onBarcodeScanned = { scannedCode ->
                barcode = scannedCode
                errorMessage = null
                showBarcodeScanner = false
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x3306B6D4)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = null,
                        tint = LaserCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (productToEdit == null) "Register New Product" else "Edit Product Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Product Image Selector Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F172A))
                        .border(
                            width = 1.dp,
                            color = if (imageUriString != null) LaserCyan else DarkSurfaceBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!imageUriString.isNullOrBlank()) {
                        AsyncImage(
                            model = File(imageUriString!!).takeIf { it.exists() } ?: imageUriString,
                            contentDescription = "Product Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top right badge to remove
                        IconButton(
                            onClick = { imageUriString = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC000000))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove Photo",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Bottom banner to change
                        Surface(
                            color = Color(0xCC0F172A),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = LaserCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Photo", fontSize = 11.sp, color = LaserCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = LaserCyan,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap to Add Product Photo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "Select from Gallery (Optional)",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Barcode input with direct "Scan" button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = {
                            barcode = it
                            errorMessage = null
                        },
                        label = { Text("Barcode / EAN / UPC *") },
                        placeholder = { Text("Type or scan barcode") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LaserCyan,
                            unfocusedBorderColor = DarkSurfaceBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { showBarcodeScanner = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LaserCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Product Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Product Name *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LaserCyan,
                        unfocusedBorderColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mandatory Category Selector Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (category.isBlank() && errorMessage != null) Color(0x22EF4444) else Color(0xFF0B132B),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            if (category.isBlank() && errorMessage != null) CoralRed else DarkSurfaceBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = if (category.isBlank() && errorMessage != null) CoralRed else LaserCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CHOOSE CATEGORY *",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (category.isBlank() && errorMessage != null) CoralRed else LaserCyan,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (category.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x2606B6D4)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = LaserCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = category,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LaserCyan
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Required • Please Select",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal list of categories + "+ Add Category" button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Add Category button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, LaserCyan),
                            modifier = Modifier.clickable { showAddCategoryDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                    text = "+ Add Category",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaserCyan
                                )
                            }
                        }

                        // Selectable categories chips
                        for (cat in categoriesList) {
                            val isSelected = category.equals(cat, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) LaserCyan else Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) LaserCyan else DarkSurfaceBorder
                                ),
                                modifier = Modifier.clickable {
                                    category = cat
                                    errorMessage = null
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pack / Unit field
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Pack / Unit (e.g. 500g, 1L, 1 Pc)") },
                    placeholder = { Text("500g, 1L, 1 Pc") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LaserCyan,
                        unfocusedBorderColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Row: MRP and Cost Price
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = mrpText,
                        onValueChange = {
                            mrpText = it
                            errorMessage = null
                        },
                        label = { Text("MRP (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberGold,
                            unfocusedBorderColor = DarkSurfaceBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { costPriceText = it },
                        label = { Text("Cost Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LaserCyan,
                            unfocusedBorderColor = DarkSurfaceBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SKU
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("Internal SKU / Code (Optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LaserCyan,
                        unfocusedBorderColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Label Notes / Remarks (Optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LaserCyan,
                        unfocusedBorderColor = DarkSurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Cloud Sheet Sync Notice
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3306B6D4)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = LaserCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auto-syncs entered product with Google Sheet",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage ?: "", color = CoralRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (barcode.isBlank()) {
                                errorMessage = "Barcode is required"
                                return@Button
                            }
                            if (name.isBlank()) {
                                errorMessage = "Product name is required"
                                return@Button
                            }
                            if (category.isBlank()) {
                                errorMessage = "Please choose or add a category for this product"
                                return@Button
                            }
                            val mrpVal = mrpText.toDoubleOrNull()
                            if (mrpVal == null || mrpVal <= 0.0) {
                                errorMessage = "Enter a valid MRP price"
                                return@Button
                            }

                            val product = Product(
                                id = productToEdit?.id ?: 0L,
                                barcode = barcode.trim(),
                                sku = sku.trim(),
                                name = name.trim(),
                                category = category.trim(),
                                unit = unit.ifBlank { "1 Pc" }.trim(),
                                currentMrp = mrpVal,
                                costPrice = costPriceText.toDoubleOrNull() ?: 0.0,
                                notes = notes.trim(),
                                imageUri = imageUriString,
                                lastUpdated = System.currentTimeMillis()
                            )
                            onConfirm(product)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LaserCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Product", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            existingCategories = categoriesList,
            onDismiss = { showAddCategoryDialog = false },
            onCategoryAdded = { newCat ->
                onAddCategory?.invoke(newCat)
                category = newCat
                errorMessage = null
                showAddCategoryDialog = false
            }
        )
    }
}
