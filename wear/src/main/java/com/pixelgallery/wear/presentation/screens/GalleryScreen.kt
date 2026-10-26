package com.pixelgallery.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import coil.compose.AsyncImage
import com.pixelgallery.shared.protocol.TransferConstants
import com.pixelgallery.shared.util.ByteUtils
import com.pixelgallery.wear.data.StoredPhotoRecord
import com.pixelgallery.wear.presentation.WearUiState
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun GalleryScreen(
    uiState: WearUiState,
    rotaryEvents: kotlinx.coroutines.flow.SharedFlow<Float>? = null,
    onPhotoClick: (Int) -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Request rotary focus for gallery scrolling
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        rotaryEvents?.collect { delta ->
            listState.scrollBy(delta * 40f)
        }
    }

    Scaffold(
        timeText = { TimeText() },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { event ->
                coroutineScope.launch {
                    listState.scrollBy(event.verticalScrollPixels * 1.5f)
                }
                true
            }
    ) {
        if (uiState.photos.isEmpty()) {
            // Empty State
            EmptyGalleryView(
                watchIp = uiState.watchIpAddress,
                freeStorage = uiState.freeStorageBytes,
                onOpenSettings = onOpenSettings
            )
        } else {
            // Photos Grid / Two-column rows
            val photos = uiState.photos
            val rowCount = (photos.size + 1) / 2

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 36.dp, bottom = 48.dp, start = 14.dp, end = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Chip (IP & Photo count) + Settings button
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusHeaderChip(
                            photoCount = photos.size,
                            watchIp = uiState.watchIpAddress,
                            onClick = onOpenSettings
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22242A))
                                .clickable(onClick = onOpenSettings),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFF7BD0B8),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Recent Transfer Banner
                if (uiState.recentTransferNotification != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF005140))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = uiState.recentTransferNotification,
                                color = Color(0xFFA6E8CF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 2-column rows
                items(rowCount) { rowIndex ->
                    val firstIndex = rowIndex * 2
                    val secondIndex = firstIndex + 1

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // First Photo
                        Box(modifier = Modifier.weight(1f)) {
                            ThumbnailCard(
                                record = photos[firstIndex],
                                onClick = { onPhotoClick(firstIndex) }
                            )
                        }

                        // Second Photo (if available)
                        if (secondIndex < photos.size) {
                            Box(modifier = Modifier.weight(1f)) {
                                ThumbnailCard(
                                    record = photos[secondIndex],
                                    onClick = { onPhotoClick(secondIndex) }
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Bottom Storage Footer & Developer Attribution
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenSettings)
                            .padding(top = 8.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)
                    ) {
                        Text(
                            text = "${photos.size} Photos • ${ByteUtils.formatBytes(uiState.freeStorageBytes)} free",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "PixelGallery v1.0.0 • by nikolopikolo",
                            color = Color(0xFF666666),
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThumbnailCard(
    record: StoredPhotoRecord,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E1E1E))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = File(record.thumbnailPath),
            contentDescription = record.photo.filename,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun StatusHeaderChip(
    photoCount: Int,
    watchIp: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFF22242A))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF7BD0B8))
        )
        Text(
            text = "$photoCount Photos • $watchIp",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmptyGalleryView(
    watchIp: String,
    freeStorage: Long,
    onOpenSettings: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1C22)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Collections,
                contentDescription = null,
                tint = Color(0xFF7BD0B8),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "PixelGallery",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Ready to receive photos",
            color = Color(0xFF7BD0B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF22262F))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Wi-Fi: $watchIp",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "PIN: ${TransferConstants.DEFAULT_PAIRING_PIN}",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF22262F))
                .clickable(onClick = onOpenSettings)
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFF7BD0B8),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
