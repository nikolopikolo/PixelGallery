package com.pixelgallery.mobile.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pixelgallery.mobile.ui.theme.PixelAccentError
import com.pixelgallery.mobile.ui.theme.PixelAccentSuccess
import com.pixelgallery.mobile.ui.theme.PixelAccentWarning
import com.pixelgallery.mobile.ui.theme.PixelBlueSecondary
import com.pixelgallery.mobile.ui.theme.PixelDarkSurface
import com.pixelgallery.mobile.ui.theme.PixelDarkSurfaceHigh
import com.pixelgallery.mobile.ui.theme.PixelDarkSurfaceVariant
import com.pixelgallery.mobile.ui.theme.PixelTealPrimary
import com.pixelgallery.mobile.ui.theme.PixelTextPrimary
import com.pixelgallery.mobile.ui.theme.PixelTextSecondary
import com.pixelgallery.mobile.ui.theme.PixelTextTertiary
import com.pixelgallery.mobile.viewmodel.MobileUiState
import com.pixelgallery.shared.model.PhotoTransferItem
import com.pixelgallery.shared.model.TransferItemStatus
import com.pixelgallery.shared.util.ByteUtils

@Composable
fun TransferProgressScreen(
    uiState: MobileUiState,
    onCancelTransfer: () -> Unit,
    onRetryFailed: () -> Unit,
    onStartTransfer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPhotos = uiState.selectedPhotos.size
    val completed = uiState.completedCount
    val failed = uiState.failedCount
    val overallProgress = if (totalPhotos > 0) completed.toFloat() / totalPhotos else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Transfer Status",
                    style = MaterialTheme.typography.headlineLarge,
                    color = PixelTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (uiState.isTransferring) "Transferring without quality loss..."
                    else if (completed == totalPhotos && totalPhotos > 0) "All photos transferred to watch"
                    else "Transfer Queue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.isTransferring) PixelTealPrimary else PixelTextSecondary
                )
            }
        }

        // Transfer Progress Hero Card
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
                    Column {
                        Text(
                            text = "Batch Progress",
                            style = MaterialTheme.typography.labelLarge,
                            color = PixelTextTertiary
                        )
                        Text(
                            text = "$completed / $totalPhotos Completed",
                            style = MaterialTheme.typography.titleLarge,
                            color = PixelTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.isTransferring) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PixelTealPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = PixelTealPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = ByteUtils.formatSpeed(uiState.currentSpeedBps),
                                style = MaterialTheme.typography.labelLarge,
                                color = PixelTealPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = PixelTealPrimary,
                    trackColor = PixelDarkSurfaceHigh
                )

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatPill("Completed", "$completed", PixelAccentSuccess)
                    StatPill("Failed", "$failed", if (failed > 0) PixelAccentError else PixelTextTertiary)
                    StatPill("Remaining", "${(totalPhotos - completed - failed).coerceAtLeast(0)}", PixelBlueSecondary)
                }

                // Action Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.isTransferring) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = onCancelTransfer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel Transfer")
                        }
                    } else {
                        if (failed > 0) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = onRetryFailed,
                                colors = ButtonDefaults.buttonColors(containerColor = PixelAccentError),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Failed ($failed)")
                            }
                        } else if (totalPhotos > 0 && completed < totalPhotos) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = onStartTransfer,
                                colors = ButtonDefaults.buttonColors(containerColor = PixelTealPrimary, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Transfer")
                            }
                        }
                    }
                }
            }
        }

        // Transfer Items List
        Text(
            text = "Files in Queue",
            style = MaterialTheme.typography.titleLarge,
            color = PixelTextPrimary
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(uiState.selectedPhotos) { index, item ->
                TransferItemRow(
                    item = item,
                    isCurrent = uiState.isTransferring && uiState.currentTransferIndex == index
                )
            }
        }
    }
}

@Composable
fun TransferItemRow(
    item: PhotoTransferItem,
    isCurrent: Boolean
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) PixelDarkSurfaceVariant else PixelDarkSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thumbnail
                AsyncImage(
                    model = item.uriString,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PixelDarkSurfaceHigh)
                )

                // Name and details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.filename,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelTextPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = ByteUtils.formatBytes(item.fileSize),
                            style = MaterialTheme.typography.labelLarge,
                            color = PixelTextSecondary
                        )
                        if (item.sha256.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "SHA-256 verified",
                                tint = PixelTealPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "SHA-256",
                                style = MaterialTheme.typography.labelLarge,
                                color = PixelTealPrimary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Status Indicator
                TransferStatusBadge(item.status)
            }

            // Real-time progress bar if transferring
            if (item.status == TransferItemStatus.TRANSFERRING) {
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = PixelTealPrimary,
                    trackColor = PixelDarkSurfaceHigh
                )
            }

            // Error message if failed
            val errMsg = item.errorMessage
            if (errMsg != null && item.status == TransferItemStatus.FAILED) {
                Text(
                    text = errMsg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelAccentError,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun TransferStatusBadge(status: TransferItemStatus) {
    when (status) {
        TransferItemStatus.PENDING -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = PixelTextTertiary, modifier = Modifier.size(16.dp))
                Text("Waiting", style = MaterialTheme.typography.labelLarge, color = PixelTextTertiary)
            }
        }
        TransferItemStatus.TRANSFERRING -> {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PixelTealPrimary)
        }
        TransferItemStatus.COMPLETED -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PixelAccentSuccess, modifier = Modifier.size(16.dp))
                Text("Sent", style = MaterialTheme.typography.labelLarge, color = PixelAccentSuccess)
            }
        }
        TransferItemStatus.FAILED -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Error, contentDescription = null, tint = PixelAccentError, modifier = Modifier.size(16.dp))
                Text("Failed", style = MaterialTheme.typography.labelLarge, color = PixelAccentError)
            }
        }
        TransferItemStatus.ALREADY_EXISTS -> {
            Text("On Watch", style = MaterialTheme.typography.labelLarge, color = PixelAccentWarning)
        }
        TransferItemStatus.CANCELLED -> {
            Text("Cancelled", style = MaterialTheme.typography.labelLarge, color = PixelTextTertiary)
        }
    }
}

@Composable
fun StatPill(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PixelDarkSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelLarge, color = PixelTextTertiary, fontSize = 11.sp)
    }
}
