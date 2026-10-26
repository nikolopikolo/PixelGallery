package com.pixelgallery.wear.presentation

import android.os.Bundle
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.wear.compose.material.MaterialTheme
import com.pixelgallery.wear.presentation.screens.FullscreenViewerScreen
import com.pixelgallery.wear.presentation.screens.GalleryScreen
import com.pixelgallery.wear.presentation.screens.WearSettingsScreen

private sealed interface Screen {
    data object Gallery : Screen
    data class Viewer(val index: Int) : Screen
    data object Settings : Screen
}

class MainActivity : ComponentActivity() {
    private val viewModel: WearGalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val uiState by viewModel.uiState.collectAsState()
                val selectedIdx = uiState.selectedPhotoIndex
                val showSettings = uiState.showSettings

                val currentScreen = when {
                    showSettings -> Screen.Settings
                    selectedIdx != null && selectedIdx in uiState.photos.indices -> Screen.Viewer(selectedIdx)
                    else -> Screen.Gallery
                }

                Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                    when (screen) {
                        is Screen.Settings -> {
                            BackHandler {
                                viewModel.setShowSettings(false)
                            }
                            WearSettingsScreen(
                                keepScreenAwake = uiState.keepScreenAwake,
                                onToggleKeepScreenAwake = { viewModel.setKeepScreenAwake(it) },
                                onClose = { viewModel.setShowSettings(false) }
                            )
                        }
                        is Screen.Viewer -> {
                            BackHandler {
                                viewModel.closeViewer()
                            }
                            val record = uiState.photos[screen.index]
                            FullscreenViewerScreen(
                                record = record,
                                currentIndex = screen.index,
                                totalCount = uiState.photos.size,
                                photos = uiState.photos,
                                storageManager = viewModel.storageManager,
                                rotaryEvents = viewModel.rotaryEvents,
                                keepScreenAwake = uiState.keepScreenAwake,
                                onClose = { viewModel.closeViewer() },
                                onDelete = { viewModel.deletePhoto(it) },
                                onNext = { viewModel.nextPhoto() },
                                onPrevious = { viewModel.previousPhoto() }
                            )
                        }
                        Screen.Gallery -> {
                            GalleryScreen(
                                uiState = uiState,
                                rotaryEvents = viewModel.rotaryEvents,
                                onPhotoClick = { viewModel.selectPhoto(it) },
                                onOpenSettings = { viewModel.setShowSettings(true) }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event != null && event.action == MotionEvent.ACTION_SCROLL &&
            event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)
        ) {
            val delta = -event.getAxisValue(MotionEvent.AXIS_SCROLL)
            viewModel.onRotaryInput(delta)
            return true
        }
        return super.onGenericMotionEvent(event)
    }
}
