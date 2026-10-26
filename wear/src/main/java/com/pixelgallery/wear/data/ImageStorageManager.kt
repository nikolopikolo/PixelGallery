package com.pixelgallery.wear.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.StatFs
import android.util.Log
import com.pixelgallery.shared.model.WatchPhoto
import com.pixelgallery.shared.util.CryptoUtils
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class ImageStorageManager(private val context: Context) {
    private val tag = "ImageStorageManager"
    private val dbHelper = GalleryDatabaseHelper(context)

    private val imagesDir: File = File(context.filesDir, "images").apply {
        if (!exists()) mkdirs()
    }
    private val thumbnailsDir: File = File(context.filesDir, "thumbnails").apply {
        if (!exists()) mkdirs()
    }

    fun getAllPhotos(): List<StoredPhotoRecord> {
        return dbHelper.getAllPhotos()
    }

    fun getPhotoById(id: String): StoredPhotoRecord? {
        return dbHelper.getPhotoById(id)
    }

    fun photoExistsBySha256(sha256: String): Boolean {
        return dbHelper.photoExistsBySha256(sha256)
    }

    fun getPhotoCount(): Int {
        return dbHelper.getPhotoCount()
    }

    fun getStorageInfo(): Pair<Long, Long> {
        val stat = StatFs(context.filesDir.path)
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        return Pair(freeBytes, totalBytes)
    }

    /**
     * Streams incoming image bytes directly to disk, computes SHA-256 on the fly,
     * extracts dimensions, creates an optimized thumbnail, and records into DB.
     */
    fun saveIncomingImageStream(
        filename: String,
        expectedSha256: String,
        inputStream: InputStream
    ): Result<WatchPhoto> {
        val photoId = UUID.randomUUID().toString()
        val extension = filename.substringAfterLast('.', "jpg")
        val destFile = File(imagesDir, "$photoId.$extension")
        val thumbFile = File(thumbnailsDir, "$photoId.jpg")

        return try {
            // 1. Stream to file while computing SHA-256
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(65536)
            var bytesRead: Int
            var totalBytesWritten = 0L

            java.io.BufferedOutputStream(FileOutputStream(destFile), 65536).use { fos ->
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    fos.write(buffer, 0, bytesRead)
                    digest.update(buffer, 0, bytesRead)
                    totalBytesWritten += bytesRead
                }
                fos.flush()
            }

            val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }

            // 2. Validate SHA-256 integrity if provided
            if (expectedSha256.isNotBlank() && !actualSha256.equals(expectedSha256, ignoreCase = true)) {
                destFile.delete()
                return Result.failure(Exception("SHA-256 checksum mismatch (calculated $actualSha256, expected $expectedSha256)"))
            }

            // 3. Decode image dimensions without loading full bitmap
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(destFile.absolutePath, boundsOptions)
            val width = boundsOptions.outWidth
            val height = boundsOptions.outHeight

            // 4. Generate optimized thumbnail (240x240)
            generateThumbnail(destFile, thumbFile, 240)

            val watchPhoto = WatchPhoto(
                id = photoId,
                filename = filename,
                width = width,
                height = height,
                fileSize = totalBytesWritten,
                addedTimestamp = System.currentTimeMillis(),
                sha256 = actualSha256
            )

            // 5. Save to database
            val record = StoredPhotoRecord(
                photo = watchPhoto,
                originalPath = destFile.absolutePath,
                thumbnailPath = thumbFile.absolutePath
            )
            dbHelper.insertPhoto(record)

            Log.d(tag, "Successfully saved photo ${watchPhoto.filename} (${watchPhoto.fileSize} bytes)")
            Result.success(watchPhoto)
        } catch (e: Exception) {
            Log.e(tag, "Failed to save incoming image", e)
            if (destFile.exists()) destFile.delete()
            if (thumbFile.exists()) thumbFile.delete()
            Result.failure(e)
        }
    }

    private fun generateThumbnail(sourceFile: File, thumbFile: File, targetSize: Int) {
        try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(sourceFile.absolutePath, boundsOptions)
            val maxDim = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
            var sampleSize = 1
            while (maxDim / (sampleSize * 2) >= targetSize) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Conserves memory
            }

            val sampledBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions) ?: return

            // Handle EXIF rotation if present
            val rotatedBitmap = rotateBitmapIfRequired(sampledBitmap, sourceFile.absolutePath)

            // Center crop to square thumbnail
            val dimension = minOf(rotatedBitmap.width, rotatedBitmap.height)
            val xOffset = (rotatedBitmap.width - dimension) / 2
            val yOffset = (rotatedBitmap.height - dimension) / 2
            val cropped = Bitmap.createBitmap(rotatedBitmap, xOffset, yOffset, dimension, dimension)

            val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)

            FileOutputStream(thumbFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            if (cropped != rotatedBitmap) cropped.recycle()
            if (scaled != cropped) scaled.recycle()
            if (rotatedBitmap != sampledBitmap) rotatedBitmap.recycle()
            sampledBitmap.recycle()
        } catch (e: Exception) {
            Log.e(tag, "Error generating thumbnail", e)
        }
    }

    // In-memory cache for fast, stutter-free photo viewing and swipe transitions
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = (maxMemoryKb / 4).coerceIn(10240, 30720) // 10MB to 30MB
    private val bitmapCache = object : android.util.LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun deletePhotos(ids: List<String>): Int {
        val deleted = dbHelper.deletePhotos(ids)
        for (record in deleted) {
            bitmapCache.remove(record.originalPath)
            File(record.originalPath).delete()
            File(record.thumbnailPath).delete()
        }
        return deleted.size
    }

    /**
     * Loads a downsampled bitmap suitable for the circular 456x456 Pixel Watch 3 display.
     * Uses LRU in-memory cache and RGB_565 to eliminate GC stalls.
     */
    fun loadOptimalBitmap(filePath: String, maxDimension: Int = 720): Bitmap? {
        val cached = bitmapCache.get(filePath)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        val maxSrc = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxSrc / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565 // 50% less RAM, optimal for watch OLED
        }
        val decoded = BitmapFactory.decodeFile(filePath, decodeOptions) ?: return null
        val rotated = rotateBitmapIfRequired(decoded, filePath)
        bitmapCache.put(filePath, rotated)
        return rotated
    }

    /**
     * Preloads adjacent photos asynchronously so swipe transitions are instant.
     */
    fun preloadAdjacentPhotos(currentIndex: Int, photos: List<StoredPhotoRecord>) {
        if (photos.isEmpty()) return
        if (currentIndex > 0) {
            val prev = photos[currentIndex - 1]
            if (bitmapCache.get(prev.originalPath) == null) {
                loadOptimalBitmap(prev.originalPath, maxDimension = 720)
            }
        }
        if (currentIndex < photos.size - 1) {
            val next = photos[currentIndex + 1]
            if (bitmapCache.get(next.originalPath) == null) {
                loadOptimalBitmap(next.originalPath, maxDimension = 720)
            }
        }
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, path: String): Bitmap {
        return try {
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val angle = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (angle != 0f) {
                val matrix = Matrix().apply { postRotate(angle) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) {
                    bitmap.recycle()
                }
                rotated
            } else {
                bitmap
            }
        } catch (_: Exception) {
            bitmap
        }
    }
}
