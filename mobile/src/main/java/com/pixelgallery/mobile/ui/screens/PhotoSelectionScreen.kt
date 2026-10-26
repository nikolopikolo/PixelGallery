package com.pixelgallery.mobile.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.pixelgallery.shared.model.PhotoTransferItem
import com.pixelgallery.shared.util.ByteUtils

@Composable
fun PhotoSelectionScreen(
    uiState: MobileUiState,
    onAddPhotos: (List<Uri>) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onClearAll: () -> Unit,
    onStartTransfer: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Android Photo Picker Launcher (Pick multiple images, no runtime permissions required)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAddPhotos(uris)
        }
    }

    val totalBytes = uiState.selectedPhotos.sumOf { it.fileSize }
    val isConnected = uiState.connectedWatch != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Action Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Photo Selection",
                    style = MaterialTheme.typography.headlineLarge,
                    color = PixelTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${uiState.selectedPhotos.size} photos selected (${ByteUtils.formatBytes(totalBytes)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelTealPrimary
                )
            }

            if (uiState.selectedPhotos.isNotEmpty()) {
                IconButtonCard(
                    icon = Icons.Default.DeleteSweep,
                    contentDescription = "Clear All",
                    onClick = onClearAll
                )
            }
        }

        // Pick Photos CTA Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PixelDarkSurface)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PixelTealPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Pick Photos",
                        tint = PixelTealPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Choose from System Gallery",
                        style = MaterialTheme.typography.titleMedium,
                        color = PixelTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Select JPEG, PNG, WEBP, or HEIC full resolution",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PixelTextSecondary
                    )
                }
            }
        }

        // Selected Photos Grid or Empty State
        if (uiState.selectedPhotos.isEmpty()) {
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
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = PixelTextTertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "No Photos Selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = PixelTextSecondary
                    )
                    Text(
                        text = "Tap above to pick photos to transfer to your Pixel Watch 3",
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
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.selectedPhotos, key = { it.uriString }) { item ->
                    SelectedPhotoCard(
                        item = item,
                        onRemove = { onRemovePhoto(item.uriString) }
                    )
                }
            }
        }

        // Bottom Transfer Action Bar
        if (uiState.selectedPhotos.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PixelDarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        onClick = onStartTransfer,
                        enabled = isConnected && !uiState.isTransferring,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PixelTealPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isConnected) "Transfer ${uiState.selectedPhotos.size} Photos to Watch"
                            else "Connect Watch to Transfer",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    if (!isConnected) {
                        Text(
                            text = "Please connect to your Pixel Watch 3 on Wi-Fi before transferring.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PixelAccentError
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedPhotoCard(
    item: PhotoTransferItem,
    onRemove: () -> Unit
) {
    Card(
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
                    model = item.uriString,
                    contentDescription = item.filename,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Remove Button overlay
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(onClick = onRemove)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Resolution Badge
                if (item.width > 0 && item.height > 0) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .align(Alignment.BottomStart)
                    ) {
                        Text(
                            text = "${item.width}×${item.height}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.filename,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = ByteUtils.formatBytes(item.fileSize),
                    style = MaterialTheme.typography.labelLarge,
                    color = PixelTextSecondary
                )
            }
        }
    }
}
