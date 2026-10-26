package com.pixelgallery.mobile.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixelgallery.mobile.data.PhotoPickerHelper
import com.pixelgallery.mobile.network.DuplicatePhotoException
import com.pixelgallery.mobile.network.NsdDiscoveryManager
import com.pixelgallery.mobile.network.WatchApiClient
import com.pixelgallery.shared.model.PhotoTransferItem
import com.pixelgallery.shared.model.TransferItemStatus
import com.pixelgallery.shared.model.WatchDeviceInfo
import com.pixelgallery.shared.model.WatchPhoto
import com.pixelgallery.shared.protocol.TransferConstants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ScreenTab {
    DASHBOARD,
    PHOTO_SELECTION,
    TRANSFER_PROGRESS,
    WATCH_GALLERY,
    SETTINGS
}

data class MobileUiState(
    val currentTab: ScreenTab = ScreenTab.DASHBOARD,
    val connectedWatch: WatchDeviceInfo? = null,
    val isConnecting: Boolean = false,
    val authToken: String? = null,
    val pairingError: String? = null,
    val isDiscovering: Boolean = false,
    val discoveredDevices: List<WatchDeviceInfo> = emptyList(),
    val showDeviceDialog: Boolean = false,
    val showManualConnectDialog: Boolean = false,

    // Photo selection and transfer
    val selectedPhotos: List<PhotoTransferItem> = emptyList(),
    val isTransferring: Boolean = false,
    val currentSpeedBps: Long = 0L,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val currentTransferIndex: Int = 0,

    // Watch gallery management
    val watchPhotos: List<WatchPhoto> = emptyList(),
    val selectedWatchPhotoIds: Set<String> = emptySet(),
    val isLoadingWatchGallery: Boolean = false,
    val galleryError: String? = null,

    // Local device info
    val localIpAddress: String = "192.168.1.100",
    val defaultPin: String = TransferConstants.DEFAULT_PAIRING_PIN
)

class MobileViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "MobileViewModel"
    private val nsdManager = NsdDiscoveryManager(application)
    private val apiClient = WatchApiClient(application)
    private val photoHelper = PhotoPickerHelper(application)

    private val _uiState = MutableStateFlow(MobileUiState())
    val uiState: StateFlow<MobileUiState> = _uiState.asStateFlow()

    private var transferJob: Job? = null
    private var speedTrackingJob: Job? = null
    private var lastBytesTransferred = 0L
    private var lastSpeedCheckTime = 0L

    init {
        // Collect discovered devices
        viewModelScope.launch {
            nsdManager.discoveredDevices.collect { devices ->
                _uiState.update { it.copy(discoveredDevices = devices) }
            }
        }
        viewModelScope.launch {
            nsdManager.isDiscovering.collect { isDiscovering ->
                _uiState.update { it.copy(isDiscovering = isDiscovering) }
            }
        }
    }

    fun setTab(tab: ScreenTab) {
        _uiState.update { it.copy(currentTab = tab) }
        if (tab == ScreenTab.WATCH_GALLERY && _uiState.value.connectedWatch != null) {
            loadWatchGallery()
        }
    }

    fun startDiscovery() {
        nsdManager.startDiscovery()
    }

    fun stopDiscovery() {
        nsdManager.stopDiscovery()
    }

    fun openDeviceDialog(open: Boolean) {
        _uiState.update { it.copy(showDeviceDialog = open) }
        if (open) startDiscovery() else stopDiscovery()
    }

    fun openManualConnectDialog(open: Boolean) {
        _uiState.update { it.copy(showManualConnectDialog = open) }
    }

    fun connectToDevice(device: WatchDeviceInfo, pin: String = _uiState.value.defaultPin) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, pairingError = null) }
            val pairResult = apiClient.pairWithWatch(device.host, device.port, pin)
            pairResult.onSuccess { pairResp ->
                val statusResult = apiClient.getWatchStatus(device.host, device.port)
                val connectedInfo = statusResult.getOrNull()?.let { status ->
                    device.copy(
                        deviceName = status.deviceName,
                        isConnected = true,
                        storageFreeBytes = status.storageFreeBytes,
                        storageTotalBytes = status.storageTotalBytes,
                        photoCount = status.photoCount
                    )
                } ?: device.copy(isConnected = true)

                _uiState.update {
                    it.copy(
                        connectedWatch = connectedInfo,
                        authToken = pairResp.authToken,
                        isConnecting = false,
                        showDeviceDialog = false,
                        showManualConnectDialog = false,
                        pairingError = null
                    )
                }
                loadWatchGallery()
            }.onFailure { error ->
                Log.e(tag, "Pairing failed", error)
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        pairingError = error.message ?: "Failed to connect to watch"
                    )
                }
            }
        }
    }

    fun connectManual(host: String, portStr: String, pin: String) {
        val port = portStr.toIntOrNull() ?: TransferConstants.DEFAULT_PORT
        val manualDevice = WatchDeviceInfo(
            deviceName = "Pixel Watch 3 (Manual)",
            host = host.trim(),
            port = port
        )
        connectToDevice(manualDevice, pin)
    }

    fun disconnect() {
        _uiState.update {
            it.copy(
                connectedWatch = null,
                authToken = null,
                watchPhotos = emptyList(),
                selectedWatchPhotoIds = emptySet()
            )
        }
    }

    fun refreshWatchStatus() {
        val watch = _uiState.value.connectedWatch ?: return
        viewModelScope.launch {
            apiClient.getWatchStatus(watch.host, watch.port).onSuccess { status ->
                _uiState.update { state ->
                    state.copy(
                        connectedWatch = watch.copy(
                            deviceName = status.deviceName,
                            photoCount = status.photoCount,
                            storageFreeBytes = status.storageFreeBytes,
                            storageTotalBytes = status.storageTotalBytes
                        )
                    )
                }
            }
        }
    }

    fun addSelectedPhotos(uris: List<Uri>) {
        viewModelScope.launch {
            val newItems = uris.map { uri -> photoHelper.resolvePhotoItem(uri) }
            _uiState.update { state ->
                val existingUris = state.selectedPhotos.map { it.uriString }.toSet()
                val distinctNew = newItems.filter { it.uriString !in existingUris }
                state.copy(selectedPhotos = state.selectedPhotos + distinctNew)
            }
        }
    }

    fun removeSelectedPhoto(uriString: String) {
        _uiState.update { state ->
            state.copy(selectedPhotos = state.selectedPhotos.filter { it.uriString != uriString })
        }
    }

    fun clearSelectedPhotos() {
        _uiState.update { it.copy(selectedPhotos = emptyList()) }
    }

    fun startTransfer() {
        val watch = _uiState.value.connectedWatch
        val token = _uiState.value.authToken
        if (watch == null || token == null) {
            _uiState.update { it.copy(pairingError = "Please connect to Pixel Watch first") }
            return
        }

        if (_uiState.value.selectedPhotos.isEmpty()) return

        transferJob?.cancel()
        _uiState.update {
            it.copy(
                isTransferring = true,
                currentTab = ScreenTab.TRANSFER_PROGRESS,
                completedCount = 0,
                failedCount = 0
            )
        }

        transferJob = viewModelScope.launch {
            val items = _uiState.value.selectedPhotos
            lastBytesTransferred = 0L
            lastSpeedCheckTime = System.currentTimeMillis()

            for ((index, item) in items.withIndex()) {
                if (!isActive) break

                _uiState.update { it.copy(currentTransferIndex = index) }

                // Update item status to TRANSFERRING
                updateItemStatus(item.uriString, TransferItemStatus.TRANSFERRING, 0f)

                var itemBytesWritten = 0L
                val uploadResult = apiClient.streamUploadPhoto(
                    host = watch.host,
                    port = watch.port,
                    authToken = token,
                    item = item
                ) { bytesWritten, totalBytes ->
                    itemBytesWritten = bytesWritten
                    val progress = if (totalBytes > 0) bytesWritten.toFloat() / totalBytes else 0f
                    updateItemStatus(item.uriString, TransferItemStatus.TRANSFERRING, progress)

                    // Calculate live speed
                    val now = System.currentTimeMillis()
                    val timeDelta = now - lastSpeedCheckTime
                    if (timeDelta >= 500) {
                        val bytesDelta = (lastBytesTransferred + bytesWritten) - lastBytesTransferred
                        val speedBps = if (timeDelta > 0) (bytesDelta * 1000) / timeDelta else 0L
                        _uiState.update { it.copy(currentSpeedBps = speedBps) }
                        lastSpeedCheckTime = now
                    }
                }

                lastBytesTransferred += itemBytesWritten

                uploadResult.onSuccess {
                    updateItemStatus(item.uriString, TransferItemStatus.COMPLETED, 1f)
                    _uiState.update { it.copy(completedCount = it.completedCount + 1) }
                }.onFailure { error ->
                    if (error is DuplicatePhotoException) {
                        updateItemStatus(item.uriString, TransferItemStatus.ALREADY_EXISTS, 1f, "Already on watch")
                    } else {
                        updateItemStatus(item.uriString, TransferItemStatus.FAILED, 0f, error.message)
                        _uiState.update { it.copy(failedCount = it.failedCount + 1) }
                    }
                }
            }

            _uiState.update { it.copy(isTransferring = false, currentSpeedBps = 0L) }
            refreshWatchStatus()
            loadWatchGallery()
        }
    }

    private fun updateItemStatus(
        uriString: String,
        status: TransferItemStatus,
        progress: Float,
        errorMessage: String? = null
    ) {
        _uiState.update { state ->
            val updated = state.selectedPhotos.map { photo ->
                if (photo.uriString == uriString) {
                    photo.copy(status = status, progress = progress, errorMessage = errorMessage)
                } else {
                    photo
                }
            }
            state.copy(selectedPhotos = updated)
        }
    }

    fun cancelTransfer() {
        transferJob?.cancel()
        _uiState.update { state ->
            val updated = state.selectedPhotos.map { photo ->
                if (photo.status == TransferItemStatus.TRANSFERRING) {
                    photo.copy(status = TransferItemStatus.CANCELLED)
                } else photo
            }
            state.copy(isTransferring = false, currentSpeedBps = 0L, selectedPhotos = updated)
        }
    }

    fun retryFailed() {
        val watch = _uiState.value.connectedWatch ?: return
        val token = _uiState.value.authToken ?: return

        transferJob?.cancel()
        _uiState.update { it.copy(isTransferring = true) }

        transferJob = viewModelScope.launch {
            val failedItems = _uiState.value.selectedPhotos.filter {
                it.status == TransferItemStatus.FAILED || it.status == TransferItemStatus.CANCELLED
            }

            for (item in failedItems) {
                if (!isActive) break

                updateItemStatus(item.uriString, TransferItemStatus.TRANSFERRING, 0f)

                val result = apiClient.streamUploadPhoto(
                    host = watch.host,
                    port = watch.port,
                    authToken = token,
                    item = item
                ) { bytesWritten, totalBytes ->
                    val progress = if (totalBytes > 0) bytesWritten.toFloat() / totalBytes else 0f
                    updateItemStatus(item.uriString, TransferItemStatus.TRANSFERRING, progress)
                }

                result.onSuccess {
                    updateItemStatus(item.uriString, TransferItemStatus.COMPLETED, 1f)
                    _uiState.update { it.copy(completedCount = it.completedCount + 1, failedCount = (it.failedCount - 1).coerceAtLeast(0)) }
                }.onFailure { error ->
                    updateItemStatus(item.uriString, TransferItemStatus.FAILED, 0f, error.message)
                }
            }

            _uiState.update { it.copy(isTransferring = false, currentSpeedBps = 0L) }
            refreshWatchStatus()
        }
    }

    fun loadWatchGallery() {
        val watch = _uiState.value.connectedWatch ?: return
        val token = _uiState.value.authToken ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWatchGallery = true, galleryError = null) }
            apiClient.getWatchGallery(watch.host, watch.port, token).onSuccess { photos ->
                _uiState.update {
                    it.copy(
                        watchPhotos = photos,
                        isLoadingWatchGallery = false,
                        selectedWatchPhotoIds = emptySet()
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingWatchGallery = false,
                        galleryError = error.message ?: "Failed to load watch photos"
                    )
                }
            }
        }
    }

    fun toggleWatchPhotoSelection(id: String) {
        _uiState.update { state ->
            val set = state.selectedWatchPhotoIds.toMutableSet()
            if (id in set) set.remove(id) else set.add(id)
            state.copy(selectedWatchPhotoIds = set)
        }
    }

    fun selectAllWatchPhotos() {
        _uiState.update { state ->
            val allIds = state.watchPhotos.map { it.id }.toSet()
            val newSet = if (state.selectedWatchPhotoIds.size == allIds.size) emptySet() else allIds
            state.copy(selectedWatchPhotoIds = newSet)
        }
    }

    fun deleteSelectedWatchPhotos() {
        val watch = _uiState.value.connectedWatch ?: return
        val token = _uiState.value.authToken ?: return
        val toDelete = _uiState.value.selectedWatchPhotoIds.toList()
        if (toDelete.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWatchGallery = true) }
            apiClient.deletePhotos(watch.host, watch.port, token, toDelete).onSuccess {
                loadWatchGallery()
                refreshWatchStatus()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingWatchGallery = false,
                        galleryError = error.message ?: "Failed to delete photos"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        nsdManager.stopDiscovery()
        transferJob?.cancel()
    }
}
