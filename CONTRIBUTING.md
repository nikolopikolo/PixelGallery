# Contributing to PixelGallery

Thank you for your interest in contributing to **PixelGallery**! We welcome contributions to make local photo transfers and wearable photo browsing even better.

## Development Workflow

1. **Fork the repository** on GitHub.
2. **Clone your fork**:
   ```bash
   git clone https://github.com/<your-username>/PixelGallery.git
   cd PixelGallery
   ```
3. **Open in Android Studio**:
   - Android Studio Hedgehog / Ladybug or newer.
   - JDK 17 or JDK 21.
4. **Create a topic branch**:
   ```bash
   git checkout -b feature/my-new-feature
   ```
5. **Run the build**:
   ```bash
   ./gradlew assembleDebug
   ```
6. **Submit a Pull Request**:
   - Provide a clear description of the feature or bug fix.
   - Mention tested device models (e.g., Pixel 6 Pro, Pixel Watch 3).

## Guidelines

- Follow official Kotlin coding style and modern Jetpack Compose best practices.
- Preserve zero-cloud privacy: all communications must remain local Wi-Fi only.
- Maintain smooth performance on Wear OS (watch memory constraints, circular layout optimizations).

Developer: **nikolopikolo** ([@nikolopikolo](https://github.com/nikolopikolo))
