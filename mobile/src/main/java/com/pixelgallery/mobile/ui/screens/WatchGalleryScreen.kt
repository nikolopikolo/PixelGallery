package com.pixelgallery.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pixelgallery.mobile.ui.theme.PixelAccentError
import com.pixelgallery.mobile.ui.theme.PixelBlueSecondary
import com.pixelgallery.mobile.ui.theme.PixelDarkSurface
import com.pixelgallery.mobile.ui.theme.PixelDarkSurfaceHigh
import com.pixelgallery.mobile.ui.theme.PixelDarkSurfaceVariant
import com.pixelgallery.mobile.ui.theme.PixelTealPrimary
import com.pixelgallery.mobile.ui.theme.PixelTextPrimary
import com.pixelgallery.mobile.ui.theme.PixelTextSecondary
import com.pixelgallery.mobile.ui.theme.PixelTextTertiary
import com.pixelgallery.mobile.viewmodel.MobileUiState
import com.pixelgallery.shared.model.WatchPhoto
import com.pixelgallery.shared.protocol.TransferConstants
import com.pixelgallery.shared.util.ByteUtils

@Composable
fun WatchGalleryScreen(
    uiState: MobileUiState,
    onRefresh: () -> Unit,
    onToggleSelectPhoto: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = uiState.connectedWatch != null
    val selectedIds = uiState.selectedWatchPhotoIds
    val watch = uiState.connectedWatch

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Watch Gallery",
                    style = MaterialTheme.typography.headlineLarge,
                    color = PixelTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isConnected) "${uiState.watchPhotos.size} photos on ${watch?.deviceName}"
                    else "Connect watch to browse photos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isConnected) PixelBlueSecondary else PixelAccentError
                )
            }

            if (isConnected) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.watchPhotos.isNotEmpty()) {
                        IconButtonCard(
                            icon = Icons.Default.SelectAll,
                            contentDescription = "Select All",
                            onClick = onSelectAll
                        )
                    }

                    IconButtonCard(
                        icon = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        onClick = onRefresh
                    )
                }
            }
        }

        // Selection Action Bar (when photos are selected)
        if (selectedIds.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PixelDarkSurfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedIds.size} Selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = PixelTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Button(
                        onClick = onDeleteSelected,
                        colors = ButtonDefaults.buttonColors(containerColor = PixelAccentError),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete (${selectedIds.size})")
                    }
                }
            }
        }

        // Gallery Content
        if (!isConnected) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = PixelTextTertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "Watch Disconnected",
                        style = MaterialTheme.typography.titleMedium,
                        color = PixelTextSecondary
                    )
                    Text(
                        text = "Connect to your Pixel Watch 3 to manage its offline gallery",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelTextTertiary
                    )
                }
            }
        } else if (uiState.isLoadingWatchGallery) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PixelTealPrimary)
            }
        } else if (uiState.watchPhotos.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = PixelTextTertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "No Photos on Watch",
                        style = MaterialTheme.typography.titleMedium,
                        color = PixelTextSecondary
                    )
                    Text(
                        text = "Transfer photos from the selection tab to view them on watch",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelTextTertiary
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.watchPhotos, key = { it.id }) { photo ->
                    val isSelected = photo.id in selectedIds
                    WatchPhotoGridItem(
                        photo = photo,
                        watchHost = watch?.host ?: "",
                        watchPort = watch?.port ?: TransferConstants.DEFAULT_PORT,
                        isSelected = isSelected,
                        onClick = { onToggleSelectPhoto(photo.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun WatchPhotoGridItem(
    photo: WatchPhoto,
    watchHost: String,
    watchPort: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val thumbUrl = "http://$watchHost:$watchPort${TransferConstants.ENDPOINT_THUMBNAIL}?id=${photo.id}"

    Card(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(2.dp, PixelTealPrimary, RoundedCornerShape(14.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PixelDarkSurface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = photo.filename,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Selection check indicator
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) PixelTealPrimary else Color.Black.copy(alpha = 0.5f)
                        )
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Resolution badge
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Text(
                        text = "${photo.width}×${photo.height}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = photo.filename,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = ByteUtils.formatBytes(photo.fileSize),
                    style = MaterialTheme.typography.labelLarge,
                    color = PixelTextSecondary
                )
            }
        }
    }
}
