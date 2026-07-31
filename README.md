# WiFi Camera Streaming

[![Patreon](https://c5.patreon.com/external/logo/become_a_patron_button.png)](https://www.patreon.com/cw/shibisty)

An Android application for remotely controlling another device's camera over a local Wi-Fi network. Each app instance acts both as a **camera** and as a **remote controller** for other devices on the same network.

## Features

- 📷 Capture photos and save them to the device's gallery
- 📡 Discover other devices running the same app on the local Wi-Fi network (no Internet or central server required)
- 🎥 Live video streaming from the remote camera before taking a photo
- 🌍 Localization: English (default) and Ukrainian, automatically selected based on the system language
- 🌗 Automatic light/dark theme that follows the device's system theme

## How It Works

1. On startup, the app registers itself on the local network using **NSD (Network Service Discovery)** with a unique service name and starts a lightweight **Ktor server** on port `9865`.
2. The **"Devices"** screen displays all other discovered devices on the network (excluding itself).
3. When a device is selected, the remote control screen opens. A **WebSocket connection** is established to receive the live video stream, while pressing the **"Take Photo"** button sends an HTTP request to `/capture`. The remote device processes the request, captures a photo, saves it to its own gallery, and returns a preview image to the initiating device.

```
Device A (Controller)                  Device B (Camera)
┌────────────────────┐                   ┌───────────────────┐
│  DeviceListScreen  │───────── NSD ───▶ │   Registered      │
│                    │                   │   on the network  │
├────────────────────┤                   ├───────────────────┤
│ RemoteControlScreen│◀──── WS /stream ──│  StreamServer     │
│   (Live Preview)   │                   │ (Camera frames)   │
│                    │─── POST /capture ▶│  Captures photo,  │
│                    │◀──  JPEG bytes ───│  saves it to      │
└────────────────────┘                   │  the gallery      │
                                         └───────────────────┘
```

## Technology Stack

| Component | Purpose |
|---|---|
| Kotlin + Jetpack Compose | UI, declarative interface |
| Navigation Compose | Navigation between screens |
| CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`) | Camera access and frame capture |
| NsdManager (Android SDK) | Local network device discovery (mDNS/DNS-SD) |
| Ktor Server (CIO engine) | Local server on the camera device (HTTP + WebSocket) |
| Ktor Client (CIO engine) | Connects to another device's server |
| MediaStore API | Saves photos to the system gallery |
| Coroutines / Flow | Asynchronous operations and live frame streaming |

## Project Structure

```
app/src/main/java/com/wificamerastreaming/
├── camera/
│   └── CameraStreamer.kt       — CameraX binding, photo capture, JPEG frame streaming
├── discovery/
│   ├── NsdHelper.kt            — NSD registration and device discovery
│   └── DeviceIdProvider.kt     — Generates a unique device ID
├── network/
│   ├── StreamServer.kt         — Ktor server: /capture (HTTP) + /stream (WebSocket)
│   ├── StreamClient.kt         — WebSocket client for receiving the live stream
│   └── CaptureClient.kt        — HTTP client for the "Take Photo" command
├── ui/
│   ├── camera/CameraScreen.kt        — Local camera with capture/devices buttons
│   ├── devices/DeviceListScreen.kt   — List of discovered devices
│   ├── remote/RemoteControlScreen.kt — Remote camera control with live preview
│   └── theme/                        — Colors, typography, light/dark themes
├── MainActivity.kt              — Entry point, permissions, NavHost, server/discovery
└── res/values(-uk)/strings.xml  — Localized strings
```

## Permissions

| Permission | Purpose | Notes |
|---|---|---|
| `CAMERA` | Photo capture and live streaming | Runtime permission requested on first launch |
| `INTERNET` | Ktor server/client communication | Standard manifest permission |
| `ACCESS_WIFI_STATE`, `ACCESS_NETWORK_STATE` | NSD functionality | Standard manifest permissions |
| `CHANGE_WIFI_MULTICAST_STATE` | Proper mDNS discovery | Standard manifest permission |
| `WRITE_EXTERNAL_STORAGE` | Saving photos to the gallery | Required only for API 24–28 (`maxSdkVersion="28"`); runtime permission. Not required on API 29+ due to scoped storage |

## Requirements

- **minSdk 21** (Android 5.0)
- **compileSdk / targetSdk 36**
- Kotlin 2.0+, JVM target 11
- Both devices must be connected to the same Wi-Fi network with **client isolation (AP isolation) disabled** on the router.

## Known Limitations

- Screen orientation is locked to portrait mode (`screenOrientation="portrait"`). This prevents active camera and network connections from being recreated during device rotation.
- The live stream consists of individual JPEG frames sent over WebSocket (~10 FPS). This is **not** a true video codec and is intended only for a live preview before taking a photo.
- Device discovery works only within the same local network (no Internet support).

## Build

```bash
./gradlew assembleDebug
```

The APK will be generated in `app/build/outputs/apk/debug/`.

## Possible Future Improvements

- H.264 video encoding instead of streaming individual JPEG frames
- Manual language selection within the app (currently follows the system language)
- Better handling of network edge cases (timeouts, connection loss, automatic reconnection)
- Support for multiple device cameras (front/rear) from the remote control screen

[![Patreon](https://c5.patreon.com/external/logo/become_a_patron_button.png)](https://www.patreon.com/cw/shibisty)

If this project helps you, consider supporting its development on Patreon ❤️
