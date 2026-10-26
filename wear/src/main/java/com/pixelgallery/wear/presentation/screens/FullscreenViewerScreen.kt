package com.pixelgallery.wear.presentation.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.pixelgallery.wear.data.ImageStorageManager
import com.pixelgallery.wear.data.StoredPhotoRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val MIN_ZOOM = 1.0f
private const val MAX_ZOOM = 5.0f
private const val DOUBLE_TAP_ZOOM = 2.5f
private const val SWIPE_THRESHOLD_PX = 75f

@Composable
fun FullscreenViewerScreen(
    record: StoredPhotoRecord,
    currentIndex: Int,
    totalCount: Int,
    photos: List<StoredPhotoRecord>,
    storageManager: ImageStorageManager,
    rotaryEvents: kotlinx.coroutines.flow.SharedFlow<Float>? = null,
    keepScreenAwake: Boolean = true,
    onClose: () -> Unit,
    onDelete: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep Screen Awake lifecycle management
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, keepScreenAwake) {
        val window = (context as? android.app.Activity)?.window
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (keepScreenAwake && window != null) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        if (keepScreenAwake && window != null) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var rawScale by remember(record.photo.id) { mutableFloatStateOf(1f) }
    var targetScale by remember(record.photo.id) { mutableFloatStateOf(1f) }
    var isAnimatedZoom by remember(record.photo.id) { mutableStateOf(false) }

    // Animated scale for double-tap zoom
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 200),
        label = "ZoomAnimation",
        finishedListener = { isAnimatedZoom = false }
    )

    val currentScale = if (isAnimatedZoom) animatedScale else rawScale

    var offset by remember(record.photo.id) { mutableStateOf(Offset.Zero) }
    var swipeDragOffset by remember(record.photo.id) { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Controls and counter auto-hide state (fades after 2 seconds)
    var showControls by remember { mutableStateOf(true) }
    var showCounter by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun markUserActivity() {
        lastInteractionTime = System.currentTimeMillis()
        showControls = true
        showCounter = true
    }

    // Auto-hide controls after 2 seconds of inactivity
    LaunchedEffect(lastInteractionTime) {
        delay(2000)
        showControls = false
        showCounter = false
    }

    // Focus Requester for Crown rotary zoom
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(record.photo.id) {
        focusRequester.requestFocus()
        rawScale = 1f
        targetScale = 1f
        offset = Offset.Zero
        swipeDragOffset = 0f
        markUserActivity()

        // Preload adjacent images asynchronously for instant swiping
        withContext(Dispatchers.IO) {
            storageManager.preloadAdjacentPhotos(currentIndex, photos)
        }
    }

    fun applyZoomDelta(delta: Float) {
        val zoomFactor = if (delta > 0) 1.20f else 0.82f
        val newScale = (rawScale * zoomFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        rawScale = newScale
        targetScale = newScale
        isAnimatedZoom = false
        markUserActivity()

        if (newScale == MIN_ZOOM) {
            offset = Offset.Zero
        } else {
            val maxOffsetX = (containerSize.width * (newScale - 1f)) / 2f
            val maxOffsetY = (containerSize.height * (newScale - 1f)) / 2f
            offset = Offset(
                x = offset.x.coerceIn(-maxOffsetX, maxOffsetX),
                y = offset.y.coerceIn(-maxOffsetY, maxOffsetY)
            )
        }
    }

    // Collect hardware rotary crown events from MainActivity
    LaunchedEffect(record.photo.id) {
        rotaryEvents?.collect { delta ->
            applyZoomDelta(delta)
        }
    }

    // Load downsampled bitmap using LRU memory cache
    val loadedBitmap by produceState<Bitmap?>(initialValue = null, key1 = record.photo.id) {
        value = withContext(Dispatchers.IO) {
            storageManager.loadOptimalBitmap(record.originalPath, maxDimension = 1080)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .focusRequester(focusRequester)
            .focusable()
            // 1. Digital Crown Rotary Zoom
            .onRotaryScrollEvent { event ->
                applyZoomDelta(-event.verticalScrollPixels)
                true
            }
            // 2. Pinch-to-zoom (2 fingers) & Panning when zoomed in
            .pointerInput(record.photo.id) {
                detectTransformGestures { _, pan, zoom, _ ->
                    markUserActivity()
                    val newScale = (rawScale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    rawScale = newScale
                    targetScale = newScale
                    isAnimatedZoom = false

                    if (newScale > 1.05f) {
                        val maxOffsetX = (containerSize.width * (newScale - 1f)) / 2f
                        val maxOffsetY = (containerSize.height * (newScale - 1f)) / 2f
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                            y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                        )
                    } else {
                        offset = Offset.Zero
                    }
                }
            }
            // 3. Single-finger drag: Swipe navigation when at fit-to-screen (zoom <= 1.05)
            .pointerInput(record.photo.id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        if (rawScale <= 1.05f) {
                            change.consume()
                            swipeDragOffset += dragAmount.x
                        }
                    },
                    onDragEnd = {
                        if (rawScale <= 1.05f) {
                            if (swipeDragOffset < -SWIPE_THRESHOLD_PX && currentIndex < totalCount - 1) {
                                onNext()
                            } else if (swipeDragOffset > SWIPE_THRESHOLD_PX && currentIndex > 0) {
                                onPrevious()
                            }
                            swipeDragOffset = 0f
                        }
                    },
                    onDragCancel = {
                        swipeDragOffset = 0f
                    }
                )
            }
            // 4. Double-tap to zoom & Tap to toggle UI controls
            .pointerInput(record.photo.id) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        markUserActivity()
                        isAnimatedZoom = true
                        if (targetScale > 1.2f) {
                            targetScale = MIN_ZOOM
                            rawScale = MIN_ZOOM
                            offset = Offset.Zero
                        } else {
                            targetScale = DOUBLE_TAP_ZOOM
                            rawScale = DOUBLE_TAP_ZOOM
                            val maxOffsetX = (containerSize.width * (DOUBLE_TAP_ZOOM - 1f)) / 2f
                            val maxOffsetY = (containerSize.height * (DOUBLE_TAP_ZOOM - 1f)) / 2f
                            val targetX = (containerSize.width / 2f - tapOffset.x) * (DOUBLE_TAP_ZOOM - 1f)
                            val targetY = (containerSize.height / 2f - tapOffset.y) * (DOUBLE_TAP_ZOOM - 1f)
                            offset = Offset(
                                x = targetX.coerceIn(-maxOffsetX, maxOffsetX),
                                y = targetY.coerceIn(-maxOffsetY, maxOffsetY)
                            )
                        }
                    },
                    onTap = {
                        showControls = !showControls
                        showCounter = showControls
                        if (showControls) lastInteractionTime = System.currentTimeMillis()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val bitmap = loadedBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = record.photo.filename,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // High-performance GPU layer: zero recomposition during pan/zoom
                        scaleX = currentScale
                        scaleY = currentScale
                        translationX = offset.x + (if (currentScale <= 1.05f) swipeDragOffset else 0f)
                        translationY = offset.y
                    },
                contentScale = ContentScale.Fit
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
                indicatorColor = Color(0xFF7BD0B8)
            )
        }

        // Minimal Photo Counter (e.g. "3 / 25") - Automatically disappears after 2s
        AnimatedVisibility(
            visible = showCounter,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${currentIndex + 1} / $totalCount",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Top Navigation Controls (Back & Delete)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, start = 14.dp, end = 14.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentScale > 1.05f) {
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1fx", currentScale)}",
                            color = Color(0xFF7BD0B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { onDelete(record.photo.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFF28B82),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom Reset Zoom Button (when zoomed in)
        if (currentScale > 1.05f) {
            Box(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable {
                        isAnimatedZoom = true
                        targetScale = 1f
                        rawScale = 1f
                        offset = Offset.Zero
                        markUserActivity()
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ZoomOutMap,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = " Fit",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
