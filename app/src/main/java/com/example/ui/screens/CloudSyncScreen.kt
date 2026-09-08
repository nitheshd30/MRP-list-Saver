package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SyncRepository
import com.example.data.repository.SyncResult
import com.example.ui.components.Card3D
import com.example.ui.components.NodeCard3D
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudSyncScreen(
    viewModel: MrpViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOnline by viewModel.isOnline.collectAsState()
    val isManualOffline by viewModel.isManualOffline.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val pendingCount by viewModel.pendingSyncCount.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val lastSyncMsg by viewModel.lastSyncMessage.collectAsState()

    var sheetUrlInput by remember {
        mutableStateOf(viewModel.syncRepository.getGoogleSheetUrl())
    }
    var autoSyncEnabled by remember {
        mutableStateOf(viewModel.syncRepository.isAutoSyncEnabled())
    }
    var syncFeedback by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy • HH:mm:ss", Locale.getDefault()) }

    // Sync rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "syncSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Box(modifier = modifier.fillMaxSize().background(DarkCanvas)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = LaserCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GOOGLE SHEETS SYNC",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Cross-device database & offline synchronization",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3D Node Connectivity & Offline Control Card
                    NodeCard3D(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 8.dp,
                        glowAccent = if (isOnline) NodeGreen else CoralRed
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) Color(0x2610B981) else Color(0x26EF4444)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = if (isOnline) NeonEmerald else CoralRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isOnline) "Connected & Online" else if (isManualOffline) "Offline Simulation Mode" else "Network Disconnected",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (pendingCount > 0) "$pendingCount offline updates queued" else "All changes synced with Room DB",
                                        fontSize = 12.sp,
                                        color = if (pendingCount > 0) AmberGold else Color(0xFF94A3B8)
                                    )
                                }

                                // Toggle manual offline switch
                                Switch(
                                    checked = !isManualOffline,
                                    onCheckedChange = { viewModel.toggleOfflineMode() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeonEmerald,
                                        checkedTrackColor = Color(0x3310B981),
                                        uncheckedThumbColor = CoralRed,
                                        uncheckedTrackColor = Color(0x33EF4444)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Toggle the switch above to simulate working offline without internet. Updates made in offline mode are safely kept in Room and will sync when you turn it back on.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sync Action Hero Card
                    Card3D(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 10.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "DATABASE RECONCILIATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (lastSyncTime > 0) "Last Synced: ${dateFormat.format(Date(lastSyncTime))}" else "Never synced yet",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1)
                            )

                            if (lastSyncMsg.isNotBlank()) {
                                Text(
                                    text = lastSyncMsg,
                                    fontSize = 11.sp,
                                    color = LaserCyan,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    syncFeedback = null
                                    viewModel.triggerSync { result ->
                                        syncFeedback = if (result.isSuccess) {
                                            "✓ ${result.message} (Pushed: ${result.pushedProducts + result.pushedHistory}, Pulled: ${result.pulledProducts})"
                                        } else {
                                            "⚠ ${result.message}"
                                        }
                                    }
                                },
                                enabled = !isSyncing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberGold,
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color(0xFF451A03),
                                    disabledContentColor = Color(0xFF94A3B8)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                if (isSyncing) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Syncing",
                                        modifier = Modifier
                                            .size(20.dp)
                                            .rotate(rotation)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Synchronizing Data...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Sync",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (pendingCount > 0) "Sync $pendingCount Pending Changes" else "Sync with Google Sheet",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (syncFeedback != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = syncFeedback ?: "",
                                    fontSize = 12.sp,
                                    color = if (syncFeedback?.startsWith("✓") == true) NeonEmerald else AmberGold,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Google Sheets Webhook Configuration
                    Text(
                        text = "GOOGLE APPS SCRIPT WEBHOOK CONFIGURATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LaserCyan,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Web App URL",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Deploy your Google Sheet with Google Apps Script as a Web App and paste the execution URL here:",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = sheetUrlInput,
                                onValueChange = { sheetUrlInput = it },
                                placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LaserCyan,
                                    unfocusedBorderColor = DarkSurfaceBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.saveGoogleSheetUrl(sheetUrlInput.trim())
                                        Toast.makeText(context, "Google Sheet URL saved!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LaserCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Save URL", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        viewModel.saveGoogleSheetUrl(sheetUrlInput.trim())
                                        isTestingConnection = true
                                        testResult = null
                                        viewModel.testGoogleSheetConnection { success, message ->
                                            isTestingConnection = false
                                            testResult = Pair(success, message)
                                        }
                                    },
                                    enabled = !isTestingConnection && sheetUrlInput.isNotBlank(),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (sheetUrlInput.isNotBlank()) LaserCyan else Color(0x33FFFFFF))
                                ) {
                                    if (isTestingConnection) {
                                        CircularProgressIndicator(
                                            color = LaserCyan,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Testing...", fontSize = 12.sp, color = LaserCyan)
                                    } else {
                                        Text("Test", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (sheetUrlInput.isNotBlank()) LaserCyan else Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Auto-Sync", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Switch(
                                        checked = autoSyncEnabled,
                                        onCheckedChange = {
                                            autoSyncEnabled = it
                                            viewModel.syncRepository.setAutoSyncEnabled(it)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = LaserCyan,
                                            checkedTrackColor = Color(0x3306B6D4)
                                        )
                                    )
                                }
                            }

                            testResult?.let { result ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (result.first) Color(0x2210B981) else Color(0x22EF4444),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (result.first) Color(0xFF10B981) else Color(0xFFEF4444)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (result.first) Icons.Default.CheckCircle else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (result.first) Color(0xFF10B981) else Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = result.second,
                                            color = if (result.first) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Google Apps Script Template Instructions
                    Text(
                        text = "GOOGLE SHEET SETUP GUIDE (FOR INCHARGE)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "3-Minute Google Sheet Cloud Setup",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "1. Open Google Sheets and create a new spreadsheet.\n" +
                                        "2. Click Extensions → Apps Script.\n" +
                                        "3. Copy and paste the script below, then click Deploy → New Deployment.\n" +
                                        "4. Select type: 'Web app', set Access to: 'Anyone', and deploy.\n" +
                                        "5. Copy the generated Web App URL and paste it into the field above!",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Google Apps Script", SyncRepository.GOOGLE_APPS_SCRIPT_TEMPLATE)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Google Apps Script code copied to clipboard!", Toast.LENGTH_LONG).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LaserCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = LaserCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Ready-to-use Google Apps Script Code", color = LaserCyan, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
