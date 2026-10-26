package com.pixelgallery.shared.protocol

object TransferConstants {
    const val NSD_SERVICE_TYPE = "_pixelgallery._tcp."
    const val NSD_SERVICE_NAME = "PixelGallery-Watch"
    const val DEFAULT_PORT = 8989

    // HTTP Endpoints
    const val ENDPOINT_STATUS = "/status"
    const val ENDPOINT_PAIR = "/pair"
    const val ENDPOINT_TRANSFER = "/transfer"
    const val ENDPOINT_GALLERY = "/gallery"
    const val ENDPOINT_DELETE = "/gallery/delete"
    const val ENDPOINT_THUMBNAIL = "/gallery/thumbnail"

    // HTTP Headers
    const val HEADER_AUTH_TOKEN = "X-PixelGallery-Auth"
    const val HEADER_FILENAME = "X-PixelGallery-Filename"
    const val HEADER_FILE_SIZE = "X-PixelGallery-Size"
    const val HEADER_SHA256 = "X-PixelGallery-SHA256"
    const val HEADER_WIDTH = "X-PixelGallery-Width"
    const val HEADER_HEIGHT = "X-PixelGallery-Height"

    // Pre-shared default PIN for pairing confirmation UI
    const val DEFAULT_PAIRING_PIN = "7493"
}
