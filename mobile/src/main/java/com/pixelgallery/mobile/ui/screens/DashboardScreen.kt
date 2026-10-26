package com.pixelgallery.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgallery.mobile.ui.theme.PixelAccentError
import com.pixelgallery.mobile.ui.theme.PixelAccentSuccess
import com.pixelgallery.mobile.ui.theme.PixelBlueSecondary
import com.pixelgallery.mobile.ui.theme.PixelDarkSurface
import com.pixelgallery.mobile.ui.theme.PixelDarkSurfaceHigh
import com.pixelgallery.mobile.ui.theme.PixelDarkSurfaceVariant
import com.pixelgallery.mobile.ui.theme.PixelTealPrimary
import com.pixelgallery.mobile.ui.theme.PixelTextPrimary
import com.pixelgallery.mobile.ui.theme.PixelTextSecondary
import com.pixelgallery.mobile.ui.theme.PixelTextTertiary
import com.pixelgallery.mobile.viewmodel.MobileUiState
import com.pixelgallery.mobile.viewmodel.ScreenTab
import com.pixelgallery.shared.util.ByteUtils

@Composable
fun DashboardScreen(
    uiState: MobileUiState,
    onNavigate: (ScreenTab) -> Unit,
    onOpenDeviceDialog: () -> Unit,
    onOpenManualConnect: () -> Unit,
    onRefreshStatus: () -> Unit,
    onPickPhotos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PixelGallery",
                    style = MaterialTheme.typography.headlineLarge,
                    color = PixelTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Companion for Pixel Watch 3",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelTealPrimary
                )
            }

            IconButtonCard(
                icon = Icons.Default.Refresh,
                contentDescription = "Refresh Status",
                onClick = onRefreshStatus
            )
        }

        // Connection Status Hero Card
        WatchConnectionHeroCard(
            uiState = uiState,
            onOpenDeviceDialog = onOpenDeviceDialog,
            onOpenManualConnect = onOpenManualConnect
        )

        // Storage & Photos Summary Card
        if (uiState.connectedWatch != null) {
            WatchStorageStatsCard(uiState = uiState)
        }

        // Active Transfer Banner (if transferring)
        if (uiState.isTransferring) {
            ActiveTransferCard(
                uiState = uiState,
                onClick = { onNavigate(ScreenTab.TRANSFER_PROGRESS) }
            )
        }

        // Quick Action Cards
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            color = PixelTextPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionTile(
                modifier = Modifier.weight(1f),
                title = "Select Photos",
                subtitle = "${uiState.selectedPhotos.size} in queue",
                icon = Icons.Default.AddPhotoAlternate,
                accentColor = PixelTealPrimary,
                onClick = onPickPhotos
            )

            ActionTile(
                modifier = Modifier.weight(1f),
                title = "Watch Gallery",
                subtitle = "${uiState.connectedWatch?.photoCount ?: 0} photos",
                icon = Icons.Default.Collections,
                accentColor = PixelBlueSecondary,
                onClick = { onNavigate(ScreenTab.WATCH_GALLERY) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionTile(
                modifier = Modifier.weight(1f),
                title = "Transfer Queue",
                subtitle = if (uiState.isTransferring) "Transferring..." else "Ready to send",
                icon = Icons.Default.Send,
                accentColor = PixelTealPrimary,
                onClick = { onNavigate(ScreenTab.TRANSFER_PROGRESS) }
            )

            ActionTile(
                modifier = Modifier.weight(1f),
                title = "Device Setup",
                subtitle = if (uiState.connectedWatch != null) "Connected" else "Disconnected",
                icon = Icons.Default.Sensors,
                accentColor = PixelBlueSecondary,
                onClick = onOpenDeviceDialog
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun WatchConnectionHeroCard(
    uiState: MobileUiState,
    onOpenDeviceDialog: () -> Unit,
    onOpenManualConnect: () -> Unit
) {
    val isConnected = uiState.connectedWatch != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PixelDarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isConnected) PixelTealPrimary.copy(alpha = 0.15f)
                                else PixelAccentError.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = "Watch",
                            tint = if (isConnected) PixelTealPrimary else PixelAccentError,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isConnected) (uiState.connectedWatch?.deviceName ?: "Pixel Watch 3")
                            else "No Watch Connected",
                            style = MaterialTheme.typography.titleLarge,
                            color = PixelTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) PixelAccentSuccess else PixelAccentError)
                            )
                            Text(
                                text = if (isConnected) "Connected over Wi-Fi" else "Tap to pair watch",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PixelTextSecondary
                            )
                        }
                    }
                }
            }

            if (isConnected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PixelDarkSurfaceVariant)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Watch IP", style = MaterialTheme.typography.labelLarge, color = PixelTextTertiary)
                        Text(
                            text = "${uiState.connectedWatch?.host}:${uiState.connectedWatch?.port}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PixelTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column {
                        Text("Phone IP", style = MaterialTheme.typography.labelLarge, color = PixelTextTertiary)
                        Text(
                            text = uiState.localIpAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PixelTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (uiState.pairingError != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = PixelAccentError)
                    Text(
                        text = uiState.pairingError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelAccentError
                    )
                }
            }

            // Connection action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onOpenDeviceDialog,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) PixelDarkSurfaceHigh else PixelTealPrimary,
                        contentColor = if (isConnected) PixelTextPrimary else Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isConnected) "Change Device" else "Scan Watch")
                }

                OutlinedButton(
                    onClick = onOpenManualConnect,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Manual IP")
                }
            }
        }
    }
}

@Composable
fun WatchStorageStatsCard(uiState: MobileUiState) {
    val watch = uiState.connectedWatch ?: return
    val freeBytes = watch.storageFreeBytes
    val totalBytes = watch.storageTotalBytes
    val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedFraction = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PixelDarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = PixelTealPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Watch Storage",
                        style = MaterialTheme.typography.titleMedium,
                        color = PixelTextPrimary
                    )
                }

                Text(
                    text = "${watch.photoCount} Photos Stored",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelBlueSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            LinearProgressIndicator(
                progress = { usedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = PixelTealPrimary,
                trackColor = PixelDarkSurfaceHigh
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${ByteUtils.formatBytes(usedBytes)} used",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelTextSecondary
                )
                Text(
                    text = "${ByteUtils.formatBytes(freeBytes)} free of ${ByteUtils.formatBytes(totalBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelTextTertiary
                )
            }
        }
    }
}

@Composable
fun ActiveTransferCard(
    uiState: MobileUiState,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PixelDarkSurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                    color = PixelTealPrimary
                )
                Column {
                    Text(
                        text = "Transferring Photos...",
                        style = MaterialTheme.typography.titleMedium,
                        color = PixelTextPrimary
                    )
                    Text(
                        text = "Speed: ${ByteUtils.formatSpeed(uiState.currentSpeedBps)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelTealPrimary
                    )
                }
            }

            Text(
                text = "${uiState.completedCount}/${uiState.selectedPhotos.size}",
                style = MaterialTheme.typography.titleLarge,
                color = PixelTextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ActionTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PixelDarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PixelTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelTextSecondary
                )
            }
        }
    }
}

@Composable
fun IconButtonCard(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(PixelDarkSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = PixelTextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}
