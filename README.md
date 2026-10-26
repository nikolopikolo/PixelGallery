# PixelGallery

**PixelGallery** is a high-performance, offline photo gallery and wireless transfer ecosystem designed natively for **Wear OS** and **Android**. It enables users to transfer full-resolution photographs directly from their smartphone to their smartwatch over local Wi-Fi, without lossy recompression, cloud services, or telemetry.

Developed by **[nikolopikolo](https://github.com/nikolopikolo)**.

---

## Key Features

### ⌚ PixelGallery Wear (Wear OS Application)
- **Circular Display Optimization**: Crafted specifically for the circular AMOLED display of the **Google Pixel Watch 3 (45mm)** with an OLED-friendly pure black background (`#000000`).
- **Complete Offline Storage**: Transferred photographs and metadata are stored persistently in an on-device SQLite database and app-specific file system. Photos remain available even when disconnected or restarted.
- **Advanced Interactive Zoom System**:
  - **Digital Crown Zoom**: Turn the watch crown clockwise to smoothly zoom in up to **5.0x**, and counter-clockwise to return to fit-to-screen (**1.0x**).
  - **Pinch-to-Zoom & Pan**: Multi-touch pinch gestures with focal-point tracking and boundary clamping.
  - **Double-Tap Zoom**: Animated toggle between 1.0x (fit screen) and 2.5x zoom.
- **Smooth Horizontal Photo Navigation**:
  - Swipe left/right between photographs when viewing at fit-to-screen zoom.
  - Panning is automatically preserved when zoomed in to prevent accidental photo switching.
  - **Instant Preloading**: Asynchronously preloads adjacent images into an in-memory LRU cache for 0ms swipe transitions.
- **Contextual UI Controls**:
  - Minimal photo counter (e.g. `3 / 25`) and navigation toolbars auto-fade after 2 seconds.
  - Tap anywhere to toggle controls.
- **Embedded Wi-Fi Server**: High-throughput streaming server running on port `8989` with Network Service Discovery (NSD/mDNS `_pixelgallery._tcp.`).

### 📱 PixelGallery Mobile (Android Companion App)
- **Native Android Photo Picker**: Select JPEG, PNG, WEBP, and HEIC photographs without granting broad storage permissions.
- **Lossless Streaming Engine**: Streams image bytes in 64 KB buffers directly from the system ContentResolver without decoding full 50MP bitmaps into phone RAM.
- **SHA-256 Verification**: Every image transfer is cryptographically verified to ensure bit-for-bit file integrity.
- **Duplicate Detection**: Identifies existing photos on the watch and prevents duplicate transfers (HTTP 409 Conflict).
- **Live Transfer Monitoring**: Real-time batch progress bar, live throughput speed (`MB/s`), and per-item status badges.
- **Remote Watch Gallery Manager**: Browse stored photos on the watch, inspect resolutions/file sizes, and execute batch deletions remotely.
- **Connection Flexibility**: Automatic mDNS local discovery with a manual IP and port fallback dialog.
- **Zero Cloud & Zero Telemetry**: All operations take place strictly within your local home Wi-Fi subnet.

---

## Supported Devices

- **Smartwatch**: Google Pixel Watch 3 (45mm), running Wear OS 4 or Wear OS 5.
- **Smartphone**: Google Pixel 6 Pro, running Android 14 / Android 15 (compatible with Android 8.0+ / API 26+).

---

## Wi-Fi Connection Guide

1. Connect both your phone and watch to the same local Wi-Fi network.
2. Open **PixelGallery Wear** on your watch. Note the IP address displayed on the screen (e.g. `192.168.1.150`).
3. Open **PixelGallery Mobile** on your phone:
   - Tap **Scan Watch** in the Dashboard to discover your watch automatically via mDNS.
   - Or tap **Manual IP**, enter the watch's IP address and pairing PIN (`7493`), then tap **Connect**.
4. Once connected, pick photos in the **Select** tab and tap **Transfer to Watch**.

---

## Installation via ADB

Ensure your devices have Developer Options and Wireless/USB Debugging enabled.

### Install on Phone (Pixel 6 Pro)
```powershell
adb install -r "mobile/build/outputs/apk/debug/mobile-debug.apk"
```

### Install on Watch (Google Pixel Watch 3)
```powershell
adb -s <watch_ip:port> install -r "wear/build/outputs/apk/debug/wear-debug.apk"
```

---

## Building from Source

### Prerequisites
- **JDK**: Java 17 or Java 21 LTS
- **Android SDK Platforms**: API 34 and API 37
- **Android SDK Build-Tools**: 36.0.0
- **Gradle**: 9.6.0 (Gradle wrapper included)
- **Android Gradle Plugin (AGP)**: 9.4.1

### Build Command
Clone the repository and run:
```bash
./gradlew assembleDebug
```

Compiled APK locations:
- Mobile Companion: `mobile/build/outputs/apk/debug/mobile-debug.apk`
- Wear OS Application: `wear/build/outputs/apk/debug/wear-debug.apk`

---

## Project Structure

```
PixelGallery/
├── mobile/                 # Android smartphone companion application
│   └── src/main/java/com/pixelgallery/mobile/
│       ├── data/           # Photo Picker helper & metadata inspection
│       ├── network/        # NSD discovery & OkHttp streaming client
│       ├── ui/screens/     # Dashboard, Selection, Transfer, Gallery, Settings
│       └── viewmodel/      # MobileUiState & reactive business logic
├── wear/                   # Native Wear OS gallery application
│   └── src/main/java/com/pixelgallery/wear/
│       ├── data/           # SQLite database & LRU memory cache
│       ├── presentation/   # Wear Compose gallery & fullscreen viewer
│       └── server/         # Embedded streaming HTTP server & NSD service
├── shared/                 # Shared Kotlin models & cryptographic utilities
│   └── src/main/java/com/pixelgallery/shared/
│       ├── model/          # Models (WatchPhoto, TransferItem, PairingRequest)
│       ├── protocol/       # Transfer constants & network headers
│       └── util/           # SHA-256 calculation & byte formatters
└── .github/workflows/      # Automated CI build workflows
```

---

## Known Limitations

- The phone and watch must be connected to the same local Wi-Fi subnet (mDNS multicast is blocked on some public guest networks).
- Extreme high-resolution panoramic photographs (e.g. >100 megapixels) may be downsampled for display on wearable screens to prevent hardware memory exhaustion.

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

Copyright (c) 2026 **nikolopikolo**.
