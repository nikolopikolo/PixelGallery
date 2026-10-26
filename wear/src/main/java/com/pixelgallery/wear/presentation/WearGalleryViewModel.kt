package com.pixelgallery.wear.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixelgallery.wear.data.ImageStorageManager
import com.pixelgallery.wear.data.StoredPhotoRecord
import com.pixelgallery.wear.server.WearTransferServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WearUiState(
    val photos: List<StoredPhotoRecord> = emptyList(),
    val selectedPhotoIndex: Int? = null,
    val watchIpAddress: String = "127.0.0.1",
    val freeStorageBytes: Long = 0L,
    val totalStorageBytes: Long = 0L,
    val recentTransferNotification: String? = null,
    val keepScreenAwake: Boolean = true,
    val showSettings: Boolean = false
)

class WearGalleryViewModel(application: Application) : AndroidViewModel(application) {
    val storageManager = ImageStorageManager(application)
    private val prefsRepo = com.pixelgallery.wear.data.WearPreferencesRepository(application)
    private var transferServer: WearTransferServer? = null

    private val _uiState = MutableStateFlow(WearUiState())
    val uiState: StateFlow<WearUiState> = _uiState.asStateFlow()

    private val _rotaryEvents = kotlinx.coroutines.flow.MutableSharedFlow<Float>(extraBufferCapacity = 20)
    val rotaryEvents: kotlinx.coroutines.flow.SharedFlow<Float> = _rotaryEvents

    fun onRotaryInput(delta: Float) {
        _rotaryEvents.tryEmit(delta)
    }

    init {
        loadPhotos()
        startServer()

        viewModelScope.launch {
            prefsRepo.keepScreenAwakeFlow.collect { enabled ->
                _uiState.update { it.copy(keepScreenAwake = enabled) }
            }
        }
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setKeepScreenAwake(enabled)
        }
    }

    fun setShowSettings(show: Boolean) {
        _uiState.update { it.copy(showSettings = show) }
    }

    private fun startServer() {
        transferServer = WearTransferServer(
            context = getApplication(),
            storageManager = storageManager,
            onPhotoReceived = { photo ->
                viewModelScope.launch {
                    _uiState.update { it.copy(recentTransferNotification = "Received ${photo.filename}") }
                    loadPhotos()
                    delay(3000)
                    _uiState.update { it.copy(recentTransferNotification = null) }
                }
            },
            onGalleryUpdated = {
                viewModelScope.launch { loadPhotos() }
            }
        )
        transferServer?.start()

        val ip = transferServer?.getLocalIpAddress() ?: "127.0.0.1"
        _uiState.update { it.copy(watchIpAddress = ip) }
    }

    fun loadPhotos() {
        val records = storageManager.getAllPhotos()
        val (free, total) = storageManager.getStorageInfo()
        _uiState.update {
            it.copy(
                photos = records,
                freeStorageBytes = free,
                totalStorageBytes = total
            )
        }
    }

    fun selectPhoto(index: Int) {
        if (index in _uiState.value.photos.indices) {
            _uiState.update { it.copy(selectedPhotoIndex = index) }
        }
    }

    fun closeViewer() {
        _uiState.update { it.copy(selectedPhotoIndex = null) }
    }

    fun nextPhoto() {
        val current = _uiState.value.selectedPhotoIndex ?: return
        val total = _uiState.value.photos.size
        if (current < total - 1) {
            _uiState.update { it.copy(selectedPhotoIndex = current + 1) }
        }
    }

    fun previousPhoto() {
        val current = _uiState.value.selectedPhotoIndex ?: return
        if (current > 0) {
            _uiState.update { it.copy(selectedPhotoIndex = current - 1) }
        }
    }

    fun deletePhoto(id: String) {
        storageManager.deletePhotos(listOf(id))
        loadPhotos()
        if (_uiState.value.photos.isEmpty()) {
            closeViewer()
        } else {
            val current = _uiState.value.selectedPhotoIndex ?: 0
            _uiState.update { it.copy(selectedPhotoIndex = current.coerceAtMost(_uiState.value.photos.size - 1)) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        transferServer?.stop()
    }
}
