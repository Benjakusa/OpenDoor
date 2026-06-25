# 🚪 OpenDoor

> **A Beautiful, Offline-First Android Media Vault & Social Video Downloader**

OpenDoor is a premium, modern Android application designed to download, organize, and play your favorite video and audio media offline. Engineered from the ground up using **100% Kotlin**, **Jetpack Compose (Material 3)**, and local SQLite persistence via **Room**, OpenDoor provides a fluid, elegant interface that operates entirely on-device.

---

## ✨ Features

*   **⚡ Social & Direct Downloader:** Paste links from TikTok, Facebook, or direct HTTP/HTTPS media URLs. The app parses and prepares files for immediate on-device download.
*   **📥 Active Download Manager:** Track ongoing downloads in real-time with precise percentage counters, current bandwidth speed indicators, and dynamic progress tracks.
*   **🔒 Secure Local Media Vault:** Save your videos and audios directly to secure local app storage, away from public galleries.
*   **🎬 Full-Featured Media Player:** Play your stored content using custom full-screen media players equipped with gesture support, dynamic playback speed controls, and quick-scrub seeking.
*   **📊 Interactive Analytics Dashboard:** Gain insights into your offline storage footprint, media distribution across platforms, and monthly download trends with clean visual analytics.
*   **🎨 Material 3 Edge-to-Edge Design:** Fully optimized with dynamic color, support for immersive dark/light modes, accessibility touch-target compliance, and responsive layouts.

---

## 🛠️ Technical Architecture

OpenDoor is built adhering to **MVVM (Model-View-ViewModel)** guidelines and **Clean Architecture** patterns:

*   **UI Layer:** 100% Jetpack Compose with custom Material Design 3 components, handling adaptive screen sizes seamlessly.
*   **State Management:** Built on Kotlin `StateFlow` and `CoroutineScope` lifecycle-aware state handling.
*   **Data Layer:** SQLite storage managed through a robust **Room Database** repository pattern, storing downloaded metadata, progress states, and platform logs.
*   **Network Engine:** Optimized **OkHttp** client and **Retrofit** integration for handling direct file size requests and secure asset streams.

---

## 🚀 Getting Started

### Prerequisites

*   **Android Studio** Jellyfish 2023.3.1 or newer
*   **Java Development Kit (JDK)** 17+
*   **Android SDK** 34+ (Target SDK 34, Min SDK 26)

### Installation & Build

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/yourusername/OpenDoor.git
    cd OpenDoor
    ```

2.  **Open in Android Studio:**
    *   Select **File > Open** and choose the cloned repository folder.
    *   Allow Gradle to synchronize and fetch the dependencies defined in the Version Catalog (`gradle/libs.versions.toml`).

3.  **Run the Project:**
    *   Connect your Android device or start a virtual emulator.
    *   Select the `app` run configuration and click the **Run** button (green arrow) in the toolbar.

---

## 📂 Project Directory Structure

```text
OpenDoor/
├── app/
│   ├── src/main/java/com/example/
│   │   ├── data/          # Room DB entities, DAOs, and repository implementations
│   │   ├── ui/
│   │   │   ├── components/# Reusable UI elements (App bars, layout containers)
│   │   │   ├── screens/   # Modular screens (Home, Player, Analytics, Admin)
│   │   │   └── theme/     # Material 3 colors, custom shapes, and typography
│   │   └── viewmodel/     # Coroutine-powered state ViewModels
│   └── build.gradle.kts   # Module-level build file
├── gradle/
│   └── libs.versions.toml # Centralized Dependency Version Catalog
└── settings.gradle.kts     # Multi-module settings configuration
```

---

## 📄 License

This project is licensed under the Apache License 2.0. Feel free to use, modify, and distribute as permitted by the terms.
