package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.MrpCatalogScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NodeGreen
import com.example.ui.theme.NodeGreenGlow
import com.example.ui.theme.NodeLime
import com.example.ui.viewmodel.MrpViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

@Composable
fun MainAppContainer(
    viewModel: MrpViewModel = viewModel()
) {
    val activeTab by viewModel.activeTab.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkCanvas,
        bottomBar = {
            Fluid3DBottomNav(
                activeTab = activeTab,
                pendingSyncCount = pendingSyncCount,
                onTabSelected = { tab ->
                    viewModel.setActiveTab(tab)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(350)) { it / 2 } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(350)) { -it / 2 } + fadeOut(tween(250)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(350)) { -it / 2 } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(350)) { it / 2 } + fadeOut(tween(250)))
                    }
                },
                label = "screenTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> MrpCatalogScreen(viewModel = viewModel)
                    1 -> ScannerScreen(viewModel = viewModel)
                    2 -> CloudSyncScreen(viewModel = viewModel)
                    else -> MrpCatalogScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun Fluid3DBottomNav(
    activeTab: Int,
    pendingSyncCount: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        NavDestination(0, "Catalog", Icons.Default.Inventory2),
        NavDestination(1, "Scanner", Icons.Default.QrCodeScanner),
        NavDestination(2, "Sync", Icons.Default.CloudSync, hasBadge = pendingSyncCount > 0, badgeText = pendingSyncCount.toString())
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            shadowElevation = 14.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        NodeGreen.copy(alpha = 0.5f),
                        NodeGreenGlow.copy(alpha = 0.25f),
                        Color(0x1A000000)
                    )
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = NodeGreen.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { item ->
                    val isSelected = activeTab == item.index

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            NodeGreen.copy(alpha = 0.28f),
                                            Color(0x1A83CD29)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onTabSelected(item.index)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (item.hasBadge) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = CoralRed,
                                            contentColor = Color.White
                                        ) {
                                            Text(item.badgeText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = if (isSelected) NodeLime else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) NodeLime else Color(0xFF64748B),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class NavDestination(
    val index: Int,
    val label: String,
    val icon: ImageVector,
    val hasBadge: Boolean = false,
    val badgeText: String = ""
)
