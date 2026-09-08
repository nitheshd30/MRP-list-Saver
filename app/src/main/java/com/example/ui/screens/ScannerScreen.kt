package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.Product
import com.example.ui.components.Card3D
import com.example.ui.components.Scanner3DView
import com.example.ui.screens.dialogs.AddEditProductDialog
import com.example.ui.screens.dialogs.ManualBarcodeDialog
import com.example.ui.screens.dialogs.UpdateMrpDialog
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LaserCyan
import com.example.ui.theme.LaserCyanGlow
import com.example.ui.theme.NeonEmerald
import com.example.ui.viewmodel.MrpViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScannerScreen(
    viewModel: MrpViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val scannedProduct by viewModel.scannedProduct.collectAsState()
    val scannedBarcode by viewModel.scannedBarcode.collectAsState()
    val productHistory by viewModel.productHistory.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    var showManualInputDialog by remember { mutableStateOf(false) }
    var showUpdateMrpDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showEditProductDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BackHandler(enabled = scannedProduct != null || scannedBarcode != null) {
        viewModel.clearScannedResult()
    }

    Box(modifier = modifier.fillMaxSize().background(DarkCanvas)) {
        if (hasCameraPermission) {
            Scanner3DView(
                onBarcodeDetected = { barcode ->
                    viewModel.onBarcodeScanned(barcode)
                },
                onManualInputClick = {
                    showManualInputDialog = true
                }
            )
        } else {
            // Permission request fallback screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0x3306B6D4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Permission",
                        tint = LaserCyan,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Camera Access Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "The camera is used to instantly scan product barcodes (EAN-13, UPC, Code 128) for real-time MRP price lookups.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = LaserCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showManualInputDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = null, tint = LaserCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enter Barcode Manually", color = LaserCyan)
                }
            }
        }

        // Instant Price Reveal Result Bottom Sheet
        AnimatedVisibility(
            visible = scannedBarcode != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = DarkSurface,
                shadowElevation = 20.dp,
                border = androidx.compose.foundation.BorderStroke(1.2.dp, LaserCyanGlow),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header row with close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0x3306B6D4),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "BARCODE LOOKUP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaserCyan,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = scannedBarcode ?: "",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = { viewModel.clearScannedResult() },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val product = scannedProduct
                    if (product != null) {
                        // Product Found State
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!product.imageUri.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, LaserCyan, RoundedCornerShape(12.dp))
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
                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp)),
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
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${product.category} • ${product.unit}" + if (product.sku.isNotBlank()) " • SKU: ${product.sku}" else "",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Big MRP Highlight
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, AmberGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "CURRENT MAXIMUM RETAIL PRICE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberGold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "₹ ${String.format(Locale.US, "%.2f", product.currentMrp)}",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = AmberGold
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Last Price Change",
                                        fontSize = 9.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = dateFormat.format(Date(product.lastUpdated)),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }

                        // Recent Price Revision History
                        if (productHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "PRICE UPDATE HISTORY (${productHistory.size} entries)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaserCyan,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            for (h in productHistory.take(2)) {
                                val diff = h.newMrp - h.previousMrp
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "₹ ${String.format(Locale.US, "%.2f", h.previousMrp)} → ₹ ${String.format(Locale.US, "%.2f", h.newMrp)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (diff != 0.0) {
                                            Text(
                                                text = String.format(Locale.US, "%+.2f", diff),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (diff > 0) CoralRed else NeonEmerald
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = h.reason,
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Actions: Update MRP & Edit Details
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showUpdateMrpDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PriceChange, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Update MRP", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            OutlinedButton(
                                onClick = { showEditProductDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LaserCyan),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = LaserCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Details", color = LaserCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Product Not Found State
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Product Not In Database",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Barcode \"$scannedBarcode\" was not recognized. Would you like to register this product now?",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { showAddProductDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = LaserCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Register Product with this Barcode", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showManualInputDialog) {
        ManualBarcodeDialog(
            onDismiss = { showManualInputDialog = false },
            onBarcodeEntered = { code ->
                viewModel.onBarcodeScanned(code)
                showManualInputDialog = false
            }
        )
    }

    if (showUpdateMrpDialog && scannedProduct != null) {
        UpdateMrpDialog(
            product = scannedProduct!!,
            onDismiss = { showUpdateMrpDialog = false },
            onConfirm = { newMrp, reason, changedBy, notes ->
                viewModel.updateMrp(scannedProduct!!, newMrp, reason, changedBy, notes)
                showUpdateMrpDialog = false
            }
        )
    }

    if (showAddProductDialog) {
        AddEditProductDialog(
            initialBarcode = scannedBarcode ?: "",
            availableCategories = categories,
            onAddCategory = { newCat ->
                viewModel.addCategory(newCat)
            },
            onDismiss = { showAddProductDialog = false },
            onConfirm = { newProduct ->
                viewModel.saveProduct(newProduct)
                Toast.makeText(context, "Product saved! Syncing with Google Sheet...", Toast.LENGTH_SHORT).show()
                showAddProductDialog = false
            }
        )
    }

    if (showEditProductDialog && scannedProduct != null) {
        AddEditProductDialog(
            productToEdit = scannedProduct,
            availableCategories = categories,
            onAddCategory = { newCat ->
                viewModel.addCategory(newCat)
            },
            onDismiss = { showEditProductDialog = false },
            onConfirm = { updatedProduct ->
                viewModel.saveProduct(updatedProduct)
                Toast.makeText(context, "Product updated! Syncing with Google Sheet...", Toast.LENGTH_SHORT).show()
                showEditProductDialog = false
            }
        )
    }
}
