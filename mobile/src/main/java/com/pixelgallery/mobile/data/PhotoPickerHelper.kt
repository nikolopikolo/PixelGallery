package com.pixelgallery.mobile.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.pixelgallery.shared.model.PhotoTransferItem
import com.pixelgallery.shared.model.TransferItemStatus
import com.pixelgallery.shared.util.CryptoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoPickerHelper(private val context: Context) {

    suspend fun resolvePhotoItem(uri: Uri): PhotoTransferItem = withContext(Dispatchers.IO) {
        var filename = "photo_${System.currentTimeMillis()}.jpg"
        var fileSize = 0L
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

        // Query ContentResolver for name and size
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) filename = name
                }
                if (sizeIndex != -1) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }

        // Decode bounds (width and height) without loading the full image into memory
        var width = 0
        var height = 0
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)
                width = options.outWidth
                height = options.outHeight
            }
        } catch (_: Exception) {
        }

        // Calculate SHA-256 checksum
        var sha256 = ""
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                sha256 = CryptoUtils.calculateSha256(stream)
            }
        } catch (_: Exception) {
        }

        PhotoTransferItem(
            uriString = uri.toString(),
            filename = filename,
            fileSize = fileSize,
            mimeType = mimeType,
            width = width,
            height = height,
            sha256 = sha256,
            status = TransferItemStatus.PENDING,
            progress = 0f
        )
    }
}
