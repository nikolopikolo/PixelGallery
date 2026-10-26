package com.pixelgallery.wear.server

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.pixelgallery.shared.model.DeletePhotosRequest
import com.pixelgallery.shared.model.DeletePhotosResponse
import com.pixelgallery.shared.model.GalleryListResponse
import com.pixelgallery.shared.model.PairingRequest
import com.pixelgallery.shared.model.PairingResponse
import com.pixelgallery.shared.model.WatchPhoto
import com.pixelgallery.shared.model.WatchStatusResponse
import com.pixelgallery.shared.protocol.TransferConstants
import com.pixelgallery.wear.data.ImageStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

class WearTransferServer(
    private val context: Context,
    private val storageManager: ImageStorageManager,
    private val onPhotoReceived: (WatchPhoto) -> Unit,
    private val onGalleryUpdated: () -> Unit
) {
    private val tag = "WearTransferServer"
    private val gson = Gson()
    private val nsdServerManager = NsdServerManager(context)

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var isRunning = false

    private var currentAuthToken: String = UUID.randomUUID().toString()

    fun start(port: Int = TransferConstants.DEFAULT_PORT) {
        if (isRunning) return
        isRunning = true

        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                    receiveBufferSize = 524288 // 512 KB socket buffer
                }
                Log.d(tag, "Wear Transfer Server started on port $port")

                nsdServerManager.registerService(port)

                while (isRunning) {
                    val socket = try {
                        serverSocket?.accept() ?: break
                    } catch (e: Exception) {
                        break
                    }
                    launch {
                        handleClient(socket)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Server error", e)
            } finally {
                stop()
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.use { s ->
                s.tcpNoDelay = true
                s.soTimeout = 60000 // 60s timeout for large photos
                s.receiveBufferSize = 524288 // 512 KB
                s.sendBufferSize = 65536

                val rawInputStream = s.getInputStream()
                val outputStream = BufferedOutputStream(s.getOutputStream(), 65536)

                // Read HTTP Header byte-by-byte until CRLF CRLF
                // This guarantees NOT A SINGLE BYTE of the image payload is pre-buffered or lost!
                val headerBaos = ByteArrayOutputStream(1024)
                var p1 = -1
                var p2 = -1
                var p3 = -1
                while (true) {
                    val b = rawInputStream.read()
                    if (b == -1) break
                    headerBaos.write(b)
                    if (p3 == '\r'.code && p2 == '\n'.code && p1 == '\r'.code && b == '\n'.code) {
                        break
                    }
                    p3 = p2
                    p2 = p1
                    p1 = b
                }

                val headerText = headerBaos.toString("US-ASCII")
                if (headerText.isBlank()) return

                val lines = headerText.split("\r\n")
                if (lines.isEmpty()) return
                val requestLine = lines[0]
                val headers = mutableMapOf<String, String>()

                for (i in 1 until lines.size) {
                    val line = lines[i]
                    val colonIdx = line.indexOf(':')
                    if (colonIdx > 0) {
                        val key = line.substring(0, colonIdx).trim().lowercase()
                        val value = line.substring(colonIdx + 1).trim()
                        headers[key] = value
                    }
                }

                val parts = requestLine.split(" ")
                if (parts.size < 2) return
                val method = parts[0].uppercase()
                val pathAndQuery = parts[1]
                val path = pathAndQuery.substringBefore('?')
                val queryString = pathAndQuery.substringAfter('?', "")

                when {
                    method == "GET" && path == TransferConstants.ENDPOINT_STATUS -> {
                        handleStatus(outputStream)
                    }
                    method == "POST" && path == TransferConstants.ENDPOINT_PAIR -> {
                        handlePair(headers, rawInputStream, outputStream)
                    }
                    method == "GET" && path == TransferConstants.ENDPOINT_GALLERY -> {
                        handleGetGallery(headers, outputStream)
                    }
                    method == "GET" && path == TransferConstants.ENDPOINT_THUMBNAIL -> {
                        val photoId = queryString.substringAfter("id=", "").substringBefore('&')
                        handleGetThumbnail(photoId, outputStream)
                    }
                    method == "POST" && path == TransferConstants.ENDPOINT_DELETE -> {
                        handleDelete(headers, rawInputStream, outputStream)
                    }
                    method == "POST" && path == TransferConstants.ENDPOINT_TRANSFER -> {
                        handleTransfer(headers, rawInputStream, outputStream)
                    }
                    else -> {
                        sendHttpResponse(outputStream, 404, "Not Found", "text/plain", "Endpoint not found")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error handling client connection", e)
        }
    }

    private fun handleStatus(out: OutputStream) {
        val (freeBytes, totalBytes) = storageManager.getStorageInfo()
        val count = storageManager.getPhotoCount()
        val deviceName = "Pixel Watch 3 (${Build.MODEL ?: "45mm"})"

        val status = WatchStatusResponse(
            deviceName = deviceName,
            photoCount = count,
            storageFreeBytes = freeBytes,
            storageTotalBytes = totalBytes
        )
        sendJsonResponse(out, 200, "OK", gson.toJson(status))
    }

    private fun handlePair(headers: Map<String, String>, inStream: InputStream, out: OutputStream) {
        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
        val bodyBytes = readExactBytes(inStream, contentLength)
        val body = String(bodyBytes, Charsets.UTF_8)
        val pairReq = try { gson.fromJson(body, PairingRequest::class.java) } catch (_: Exception) { null }

        // Accept connection with PIN
        if (pairReq != null && (pairReq.pin == TransferConstants.DEFAULT_PAIRING_PIN || pairReq.pin.isBlank())) {
            currentAuthToken = UUID.randomUUID().toString()
            val resp = PairingResponse(success = true, authToken = currentAuthToken, message = "Paired successfully")
            sendJsonResponse(out, 200, "OK", gson.toJson(resp))
        } else {
            val resp = PairingResponse(success = false, authToken = null, message = "Incorrect PIN")
            sendJsonResponse(out, 401, "Unauthorized", gson.toJson(resp))
        }
    }

    private fun handleGetGallery(headers: Map<String, String>, out: OutputStream) {
        val photos = storageManager.getAllPhotos().map { it.photo }
        val response = GalleryListResponse(photos)
        sendJsonResponse(out, 200, "OK", gson.toJson(response))
    }

    private fun handleGetThumbnail(photoId: String, out: OutputStream) {
        val record = storageManager.getPhotoById(photoId)
        if (record == null) {
            sendHttpResponse(out, 404, "Not Found", "text/plain", "Photo not found")
            return
        }
        val thumbFile = File(record.thumbnailPath)
        if (!thumbFile.exists()) {
            sendHttpResponse(out, 404, "Not Found", "text/plain", "Thumbnail not found")
            return
        }

        val length = thumbFile.length()
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: image/jpeg\r\n" +
                "Content-Length: $length\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.US_ASCII))

        FileInputStream(thumbFile).use { fis ->
            val buf = ByteArray(32768)
            var n: Int
            while (fis.read(buf).also { n = it } != -1) {
                out.write(buf, 0, n)
            }
        }
        out.flush()
    }

    private fun handleDelete(headers: Map<String, String>, inStream: InputStream, out: OutputStream) {
        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
        val bodyBytes = readExactBytes(inStream, contentLength)
        val body = String(bodyBytes, Charsets.UTF_8)
        val deleteReq = try { gson.fromJson(body, DeletePhotosRequest::class.java) } catch (_: Exception) { null }

        if (deleteReq == null || deleteReq.photoIds.isEmpty()) {
            sendJsonResponse(out, 400, "Bad Request", gson.toJson(DeletePhotosResponse(false, 0)))
            return
        }

        val deletedCount = storageManager.deletePhotos(deleteReq.photoIds)
        onGalleryUpdated()
        sendJsonResponse(out, 200, "OK", gson.toJson(DeletePhotosResponse(true, deletedCount)))
    }

    private fun handleTransfer(headers: Map<String, String>, inStream: InputStream, out: OutputStream) {
        val filename = headers[TransferConstants.HEADER_FILENAME.lowercase()] ?: "photo_${System.currentTimeMillis()}.jpg"
        val expectedSha256 = headers[TransferConstants.HEADER_SHA256.lowercase()] ?: ""
        val contentLength = headers["content-length"]?.toLongOrNull() ?: 0L

        // Check for duplicate photos by SHA-256
        if (expectedSha256.isNotBlank() && storageManager.photoExistsBySha256(expectedSha256)) {
            Log.d(tag, "Duplicate photo detected: $filename ($expectedSha256)")
            sendHttpResponse(out, 409, "Conflict", "application/json", "{\"message\":\"Photo already exists\"}")
            return
        }

        // Bounded stream matching exact Content-Length
        val boundedStream = object : InputStream() {
            private var remaining = contentLength
            override fun read(): Int {
                if (remaining <= 0) return -1
                val b = inStream.read()
                if (b != -1) remaining--
                return b
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (remaining <= 0) return -1
                val toRead = minOf(len.toLong(), remaining).toInt()
                val read = inStream.read(b, off, toRead)
                if (read != -1) remaining -= read
                return read
            }
        }

        val saveResult = storageManager.saveIncomingImageStream(filename, expectedSha256, boundedStream)
        saveResult.onSuccess { savedPhoto ->
            onPhotoReceived(savedPhoto)
            onGalleryUpdated()
            sendJsonResponse(out, 200, "OK", gson.toJson(savedPhoto))
        }.onFailure { err ->
            Log.e(tag, "Transfer failed: ${err.message}", err)
            sendHttpResponse(out, 500, "Internal Server Error", "application/json", "{\"error\":\"${err.message}\"}")
        }
    }

    private fun readExactBytes(inStream: InputStream, length: Int): ByteArray {
        val bytes = ByteArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val r = inStream.read(bytes, totalRead, length - totalRead)
            if (r == -1) break
            totalRead += r
        }
        return bytes
    }

    private fun sendJsonResponse(out: OutputStream, code: Int, status: String, json: String) {
        sendHttpResponse(out, code, status, "application/json", json)
    }

    private fun sendHttpResponse(out: OutputStream, code: Int, status: String, contentType: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $code $status\r\n" +
                "Content-Type: $contentType; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.US_ASCII))
        out.write(bytes)
        out.flush()
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        nsdServerManager.unregisterService()
        serverJob?.cancel()
    }
}
