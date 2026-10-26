package com.pixelgallery.mobile.network

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.pixelgallery.shared.model.DeletePhotosRequest
import com.pixelgallery.shared.model.DeletePhotosResponse
import com.pixelgallery.shared.model.GalleryListResponse
import com.pixelgallery.shared.model.PairingRequest
import com.pixelgallery.shared.model.PairingResponse
import com.pixelgallery.shared.model.PhotoTransferItem
import com.pixelgallery.shared.model.WatchPhoto
import com.pixelgallery.shared.model.WatchStatusResponse
import com.pixelgallery.shared.protocol.TransferConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class WatchApiClient(private val context: Context) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun getWatchStatus(host: String, port: Int): Result<WatchStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$host:$port${TransferConstants.ENDPOINT_STATUS}"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error: ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val status = gson.fromJson(body, WatchStatusResponse::class.java)
                Result.success(status)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pairWithWatch(host: String, port: Int, pin: String): Result<PairingResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$host:$port${TransferConstants.ENDPOINT_PAIR}"
            val pairingReq = PairingRequest(
                clientName = android.os.Build.MODEL ?: "Pixel Phone",
                clientIp = "",
                pin = pin
            )
            val jsonBody = gson.toJson(pairingReq)
            val reqBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)
            val request = Request.Builder()
                .url(url)
                .post(reqBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                val pairingResponse = gson.fromJson(body, PairingResponse::class.java)
                if (response.isSuccessful && pairingResponse.success) {
                    Result.success(pairingResponse)
                } else {
                    Result.failure(Exception(pairingResponse.message.ifEmpty { "Pairing failed (${response.code})" }))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWatchGallery(host: String, port: Int, authToken: String): Result<List<WatchPhoto>> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$host:$port${TransferConstants.ENDPOINT_GALLERY}"
            val request = Request.Builder()
                .url(url)
                .header(TransferConstants.HEADER_AUTH_TOKEN, authToken)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error: ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val galleryResponse = gson.fromJson(body, GalleryListResponse::class.java)
                Result.success(galleryResponse.photos)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePhotos(
        host: String,
        port: Int,
        authToken: String,
        photoIds: List<String>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$host:$port${TransferConstants.ENDPOINT_DELETE}"
            val deleteReq = DeletePhotosRequest(photoIds)
            val reqBody = RequestBody.create("application/json".toMediaTypeOrNull(), gson.toJson(deleteReq))
            val request = Request.Builder()
                .url(url)
                .header(TransferConstants.HEADER_AUTH_TOKEN, authToken)
                .post(reqBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Delete failed: ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val deleteResponse = gson.fromJson(body, DeletePhotosResponse::class.java)
                Result.success(deleteResponse.deletedCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun streamUploadPhoto(
        host: String,
        port: Int,
        authToken: String,
        item: PhotoTransferItem,
        onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
    ): Result<WatchPhoto> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(item.uriString)
            val url = "http://$host:$port${TransferConstants.ENDPOINT_TRANSFER}"

            val streamingBody = object : RequestBody() {
                override fun contentType() = item.mimeType.toMediaTypeOrNull()

                override fun contentLength(): Long = item.fileSize

                override fun writeTo(sink: BufferedSink) {
                    val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                        ?: throw Exception("Could not open input stream for $uri")

                    inputStream.use { stream ->
                        val buffer = ByteArray(65536) // 64 KB chunk
                        var totalBytesWritten = 0L
                        var bytesRead: Int

                        while (stream.read(buffer).also { bytesRead = it } != -1) {
                            if (!coroutineContext.isActive) {
                                throw Exception("Transfer cancelled by user")
                            }
                            sink.write(buffer, 0, bytesRead)
                            totalBytesWritten += bytesRead
                            onProgress(totalBytesWritten, item.fileSize)
                        }
                        sink.flush()
                    }
                }
            }

            val request = Request.Builder()
                .url(url)
                .header(TransferConstants.HEADER_AUTH_TOKEN, authToken)
                .header(TransferConstants.HEADER_FILENAME, item.filename)
                .header(TransferConstants.HEADER_FILE_SIZE, item.fileSize.toString())
                .header(TransferConstants.HEADER_SHA256, item.sha256)
                .header(TransferConstants.HEADER_WIDTH, item.width.toString())
                .header(TransferConstants.HEADER_HEIGHT, item.height.toString())
                .post(streamingBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 409) {
                    return@withContext Result.failure(DuplicatePhotoException("Photo already exists on watch"))
                }
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Server returned HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty watch response"))
                val savedPhoto = gson.fromJson(body, WatchPhoto::class.java)

                // Verify SHA-256 integrity match
                if (savedPhoto.sha256.isNotEmpty() && item.sha256.isNotEmpty() &&
                    !savedPhoto.sha256.equals(item.sha256, ignoreCase = true)
                ) {
                    return@withContext Result.failure(Exception("SHA-256 checksum mismatch! Transfer corrupted."))
                }

                Result.success(savedPhoto)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DuplicatePhotoException(message: String) : Exception(message)
