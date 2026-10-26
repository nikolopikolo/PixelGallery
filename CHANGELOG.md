# Changelog

All notable changes to the **PixelGallery** ecosystem will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-26

### Added
- **Multi-module Architecture**:
  - `:mobile`: Native Android companion app with Material 3 dark theme.
  - `:wear`: Native Wear OS gallery application optimized for Google Pixel Watch 3 (45mm).
  - `:shared`: Common models, transfer protocol definitions, and cryptographic checksum utilities.
- **Local Wi-Fi Transfer Engine**:
  - Automatic watch discovery via Android Network Service Discovery (NSD/mDNS `_pixelgallery._tcp.`).
  - Fallback manual IP and port connection dialog.
  - PIN pairing authentication.
  - High-speed 64 KB chunk streaming transfer server on port 8989.
  - SHA-256 cryptographic verification for loss-free image transfer.
  - Duplicate photo detection (HTTP 409 Conflict) preventing redundant storage.
- **Wear OS Offline Gallery**:
  - Circular display layout with OLED-friendly pure black background (`#000000`).
  - Persistent SQLite database storing dimensions, timestamps, sizes, and checksums.
  - Background 240x240 thumbnail generation and LRU in-memory bitmap cache.
- **Advanced Interactive Zoom System**:
  - **Digital Crown Zoom**: Smooth hardware rotary zoom from 1.0x (fit screen) up to 5.0x.
  - **Pinch-to-Zoom**: Multi-touch zoom and panning with boundary clamping.
  - **Double-Tap Zoom**: Animated toggle between 1.0x and 2.5x.
  - **Swipe Navigation**: Smooth horizontal single-finger swipe between photos at fit-to-screen zoom.
  - **Preloading Engine**: Automatic adjacent photo caching for 0ms swipe latency.
  - **Auto-Hiding Controls**: Contextual photo counter and toolbar controls auto-fading after 2 seconds.
- **Developer Attribution**:
  - Integrated developer attribution for **nikolopikolo** with clickable GitHub profile links.
- **Custom Application Icons**:
  - Integrated custom PixelGallery mint-green artwork for phone and watch launchers.
