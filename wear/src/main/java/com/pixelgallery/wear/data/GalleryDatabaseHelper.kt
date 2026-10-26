package com.pixelgallery.wear.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.pixelgallery.shared.model.WatchPhoto

data class StoredPhotoRecord(
    val photo: WatchPhoto,
    val originalPath: String,
    val thumbnailPath: String
)

class GalleryDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "pixelgallery_watch.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_PHOTOS = "photos"
        private const val COL_ID = "id"
        private const val COL_FILENAME = "filename"
        private const val COL_WIDTH = "width"
        private const val COL_HEIGHT = "height"
        private const val COL_FILE_SIZE = "file_size"
        private const val COL_ADDED_TIMESTAMP = "added_timestamp"
        private const val COL_SHA256 = "sha256"
        private const val COL_ORIGINAL_PATH = "original_path"
        private const val COL_THUMBNAIL_PATH = "thumbnail_path"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createQuery = """
            CREATE TABLE $TABLE_PHOTOS (
                $COL_ID TEXT PRIMARY KEY,
                $COL_FILENAME TEXT NOT NULL,
                $COL_WIDTH INTEGER NOT NULL,
                $COL_HEIGHT INTEGER NOT NULL,
                $COL_FILE_SIZE INTEGER NOT NULL,
                $COL_ADDED_TIMESTAMP INTEGER NOT NULL,
                $COL_SHA256 TEXT NOT NULL,
                $COL_ORIGINAL_PATH TEXT NOT NULL,
                $COL_THUMBNAIL_PATH TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createQuery)
        db.execSQL("CREATE INDEX idx_sha256 ON $TABLE_PHOTOS ($COL_SHA256)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PHOTOS")
        onCreate(db)
    }

    @Synchronized
    fun insertPhoto(record: StoredPhotoRecord): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, record.photo.id)
            put(COL_FILENAME, record.photo.filename)
            put(COL_WIDTH, record.photo.width)
            put(COL_HEIGHT, record.photo.height)
            put(COL_FILE_SIZE, record.photo.fileSize)
            put(COL_ADDED_TIMESTAMP, record.photo.addedTimestamp)
            put(COL_SHA256, record.photo.sha256)
            put(COL_ORIGINAL_PATH, record.originalPath)
            put(COL_THUMBNAIL_PATH, record.thumbnailPath)
        }
        val result = db.insertWithOnConflict(TABLE_PHOTOS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return result != -1L
    }

    @Synchronized
    fun photoExistsBySha256(sha256: String): Boolean {
        if (sha256.isBlank()) return false
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PHOTOS,
            arrayOf(COL_ID),
            "$COL_SHA256 = ?",
            arrayOf(sha256),
            null, null, null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    @Synchronized
    fun getAllPhotos(): List<StoredPhotoRecord> {
        val list = mutableListOf<StoredPhotoRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PHOTOS,
            null,
            null,
            null,
            null,
            null,
            "$COL_ADDED_TIMESTAMP DESC"
        )
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_ID)
            val nameIdx = it.getColumnIndexOrThrow(COL_FILENAME)
            val wIdx = it.getColumnIndexOrThrow(COL_WIDTH)
            val hIdx = it.getColumnIndexOrThrow(COL_HEIGHT)
            val sizeIdx = it.getColumnIndexOrThrow(COL_FILE_SIZE)
            val timeIdx = it.getColumnIndexOrThrow(COL_ADDED_TIMESTAMP)
            val shaIdx = it.getColumnIndexOrThrow(COL_SHA256)
            val origIdx = it.getColumnIndexOrThrow(COL_ORIGINAL_PATH)
            val thumbIdx = it.getColumnIndexOrThrow(COL_THUMBNAIL_PATH)

            while (it.moveToNext()) {
                val photo = WatchPhoto(
                    id = it.getString(idIdx),
                    filename = it.getString(nameIdx),
                    width = it.getInt(wIdx),
                    height = it.getInt(hIdx),
                    fileSize = it.getLong(sizeIdx),
                    addedTimestamp = it.getLong(timeIdx),
                    sha256 = it.getString(shaIdx)
                )
                list.add(
                    StoredPhotoRecord(
                        photo = photo,
                        originalPath = it.getString(origIdx),
                        thumbnailPath = it.getString(thumbIdx)
                    )
                )
            }
        }
        return list
    }

    @Synchronized
    fun getPhotoById(id: String): StoredPhotoRecord? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PHOTOS,
            null,
            "$COL_ID = ?",
            arrayOf(id),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val photo = WatchPhoto(
                    id = it.getString(it.getColumnIndexOrThrow(COL_ID)),
                    filename = it.getString(it.getColumnIndexOrThrow(COL_FILENAME)),
                    width = it.getInt(it.getColumnIndexOrThrow(COL_WIDTH)),
                    height = it.getInt(it.getColumnIndexOrThrow(COL_HEIGHT)),
                    fileSize = it.getLong(it.getColumnIndexOrThrow(COL_FILE_SIZE)),
                    addedTimestamp = it.getLong(it.getColumnIndexOrThrow(COL_ADDED_TIMESTAMP)),
                    sha256 = it.getString(it.getColumnIndexOrThrow(COL_SHA256))
                )
                return StoredPhotoRecord(
                    photo = photo,
                    originalPath = it.getString(it.getColumnIndexOrThrow(COL_ORIGINAL_PATH)),
                    thumbnailPath = it.getString(it.getColumnIndexOrThrow(COL_THUMBNAIL_PATH))
                )
            }
        }
        return null
    }

    @Synchronized
    fun deletePhotos(ids: List<String>): List<StoredPhotoRecord> {
        val deletedRecords = mutableListOf<StoredPhotoRecord>()
        for (id in ids) {
            getPhotoById(id)?.let { record ->
                deletedRecords.add(record)
            }
            writableDatabase.delete(TABLE_PHOTOS, "$COL_ID = ?", arrayOf(id))
        }
        return deletedRecords
    }

    @Synchronized
    fun getPhotoCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_PHOTOS", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }
}
