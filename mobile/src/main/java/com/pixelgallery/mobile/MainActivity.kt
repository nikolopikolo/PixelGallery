package com.pixelgallery.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pixelgallery.mobile.ui.screens.DashboardScreen
import com.pixelgallery.mobile.ui.screens.DeviceDiscoveryDialog
import com.pixelgallery.mobile.ui.screens.ManualConnectDialog
import com.pixelgallery.mobile.ui.screens.PhotoSelectionScreen
import com.pixelgallery.mobile.ui.screens.SettingsScreen
import com.pixelgallery.mobile.ui.screens.TransferProgressScreen
import com.pixelgallery.mobile.ui.screens.WatchGalleryScreen
import com.pixelgallery.mobile.ui.theme.PixelDarkBackground
import com.pixelgallery.mobile.ui.theme.PixelDarkSurface
import com.pixelgallery.mobile.ui.theme.PixelGalleryTheme
import com.pixelgallery.mobile.ui.theme.PixelTealPrimary
import com.pixelgallery.mobile.ui.theme.PixelTextSecondary
import com.pixelgallery.mobile.ui.theme.PixelTextTertiary
import com.pixelgallery.mobile.viewmodel.MobileViewModel
import com.pixelgallery.mobile.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {
    private val viewModel: MobileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PixelGalleryTheme {
                val uiState by viewModel.uiState.collectAsState()

                // Photo picker launcher usable from any screen (e.g. Dashboard shortcut)
                val photoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickMultipleVisualMedia()
                ) { uris ->
                    if (uris.isNotEmpty()) {
                        viewModel.addSelectedPhotos(uris)
                        viewModel.setTab(ScreenTab.PHOTO_SELECTION)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = PixelDarkBackground,
                    bottomBar = {
                        PixelBottomBar(
                            currentTab = uiState.currentTab,
                            onTabSelected = { viewModel.setTab(it) },
                            selectedCount = uiState.selectedPhotos.size,
                            isTransferring = uiState.isTransferring
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (uiState.currentTab) {
                            ScreenTab.DASHBOARD -> DashboardScreen(
                                uiState = uiState,
                                onNavigate = { viewModel.setTab(it) },
                                onOpenDeviceDialog = { viewModel.openDeviceDialog(true) },
                                onOpenManualConnect = { viewModel.openManualConnectDialog(true) },
                                onRefreshStatus = { viewModel.refreshWatchStatus() },
                                onPickPhotos = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )

                            ScreenTab.PHOTO_SELECTION -> PhotoSelectionScreen(
                                uiState = uiState,
                                onAddPhotos = { viewModel.addSelectedPhotos(it) },
                                onRemovePhoto = { viewModel.removeSelectedPhoto(it) },
                                onClearAll = { viewModel.clearSelectedPhotos() },
                                onStartTransfer = { viewModel.startTransfer() }
                            )

                            ScreenTab.TRANSFER_PROGRESS -> TransferProgressScreen(
                                uiState = uiState,
                                onCancelTransfer = { viewModel.cancelTransfer() },
                                onRetryFailed = { viewModel.retryFailed() },
                                onStartTransfer = { viewModel.startTransfer() }
                            )

                            ScreenTab.WATCH_GALLERY -> WatchGalleryScreen(
                                uiState = uiState,
                                onRefresh = { viewModel.loadWatchGallery() },
                                onToggleSelectPhoto = { viewModel.toggleWatchPhotoSelection(it) },
                                onSelectAll = { viewModel.selectAllWatchPhotos() },
                                onDeleteSelected = { viewModel.deleteSelectedWatchPhotos() }
                            )

                            ScreenTab.SETTINGS -> SettingsScreen(
                                uiState = uiState,
                                onDisconnect = { viewModel.disconnect() }
                            )
                        }

                        // Dialogs
                        DeviceDiscoveryDialog(
                            isOpen = uiState.showDeviceDialog,
                            isDiscovering = uiState.isDiscovering,
                            devices = uiState.discoveredDevices,
                            isConnecting = uiState.isConnecting,
                            onConnect = { viewModel.connectToDevice(it) },
                            onRefresh = { viewModel.startDiscovery() },
                            onDismiss = { viewModel.openDeviceDialog(false) }
                        )

                        ManualConnectDialog(
                            isOpen = uiState.showManualConnectDialog,
                            isConnecting = uiState.isConnecting,
                            onConnectManual = { host, port, pin ->
                                viewModel.connectManual(host, port, pin)
                            },
                            onDismiss = { viewModel.openManualConnectDialog(false) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PixelBottomBar(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    selectedCount: Int,
    isTransferring: Boolean
) {
    NavigationBar(
        containerColor = PixelDarkSurface,
        contentColor = PixelTextSecondary
    ) {
        NavigationBarItem(
            selected = currentTab == ScreenTab.DASHBOARD,
            onClick = { onTabSelected(ScreenTab.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = PixelTealPrimary.copy(alpha = 0.2f),
                selectedIconColor = PixelTealPrimary,
                selectedTextColor = PixelTealPrimary,
                unselectedIconColor = PixelTextTertiary,
                unselectedTextColor = PixelTextTertiary
            )
        )

        NavigationBarItem(
            selected = currentTab == ScreenTab.PHOTO_SELECTION,
            onClick = { onTabSelected(ScreenTab.PHOTO_SELECTION) },
            icon = {
                if (selectedCount > 0) {
                    BadgedBox(badge = { Badge { Text("$selectedCount") } }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Photos")
                    }
                } else {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Photos")
                }
            },
            label = { Text("Select") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = PixelTealPrimary.copy(alpha = 0.2f),
                selectedIconColor = PixelTealPrimary,
                selectedTextColor = PixelTealPrimary,
                unselectedIconColor = PixelTextTertiary,
                unselectedTextColor = PixelTextTertiary
            )
        )

        NavigationBarItem(
            selected = currentTab == ScreenTab.TRANSFER_PROGRESS,
            onClick = { onTabSelected(ScreenTab.TRANSFER_PROGRESS) },
            icon = {
                if (isTransferring) {
                    BadgedBox(badge = { Badge { Text("●") } }) {
                        Icon(Icons.Default.Send, contentDescription = "Transfer")
                    }
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Transfer")
                }
            },
            label = { Text("Transfer") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = PixelTealPrimary.copy(alpha = 0.2f),
                selectedIconColor = PixelTealPrimary,
                selectedTextColor = PixelTealPrimary,
                unselectedIconColor = PixelTextTertiary,
                unselectedTextColor = PixelTextTertiary
            )
        )

        NavigationBarItem(
            selected = currentTab == ScreenTab.WATCH_GALLERY,
            onClick = { onTabSelected(ScreenTab.WATCH_GALLERY) },
            icon = { Icon(Icons.Default.Collections, contentDescription = "Watch Gallery") },
            label = { Text("Watch") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = PixelTealPrimary.copy(alpha = 0.2f),
                selectedIconColor = PixelTealPrimary,
                selectedTextColor = PixelTealPrimary,
                unselectedIconColor = PixelTextTertiary,
                unselectedTextColor = PixelTextTertiary
            )
        )

        NavigationBarItem(
            selected = currentTab == ScreenTab.SETTINGS,
            onClick = { onTabSelected(ScreenTab.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = PixelTealPrimary.copy(alpha = 0.2f),
                selectedIconColor = PixelTealPrimary,
                selectedTextColor = PixelTealPrimary,
                unselectedIconColor = PixelTextTertiary,
                unselectedTextColor = PixelTextTertiary
            )
        )
    }
}
