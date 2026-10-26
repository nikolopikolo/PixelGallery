package com.pixelgallery.shared.model

enum class TransferItemStatus {
    PENDING,
    TRANSFERRING,
    COMPLETED,
    FAILED,
    CANCELLED,
    ALREADY_EXISTS
}

data class WatchPhoto(
    val id: String,
    val filename: String,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val addedTimestamp: Long,
    val sha256: String
)

data class WatchDeviceInfo(
    val deviceName: String,
    val host: String,
    val port: Int,
    val isConnected: Boolean = false,
    val storageFreeBytes: Long = 0L,
    val storageTotalBytes: Long = 0L,
    val photoCount: Int = 0
)

data class PairingRequest(
    val clientName: String,
    val clientIp: String,
    val pin: String
)

data class PairingResponse(
    val success: Boolean,
    val authToken: String? = null,
    val message: String = ""
)

data class WatchStatusResponse(
    val deviceName: String,
    val photoCount: Int,
    val storageFreeBytes: Long,
    val storageTotalBytes: Long
)

data class GalleryListResponse(
    val photos: List<WatchPhoto>
)

data class DeletePhotosRequest(
    val photoIds: List<String>
)

data class DeletePhotosResponse(
    val success: Boolean,
    val deletedCount: Int
)

data class PhotoTransferItem(
    val uriString: String,
    val filename: String,
    val fileSize: Long,
    val mimeType: String,
    val width: Int = 0,
    val height: Int = 0,
    val sha256: String = "",
    val status: TransferItemStatus = TransferItemStatus.PENDING,
    val progress: Float = 0f,
    val errorMessage: String? = null
)
