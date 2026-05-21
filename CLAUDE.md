# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

StateLink is a USB-based PC hardware monitor for Android. An Android phone displays real-time computer stats (CPU, memory, disk, network speed, battery) via a USB data cable — no WiFi, Bluetooth, or internet required.

**Architecture:**
```
┌──────────────┐    USB cable     ┌──────────────────┐
│  Android App  │ ◄──ADB reverse──│  Python Server    │
│  (Java)       │   tcp:8765      │  (psutil + http)  │
│  localhost:   │                 │  collects CPU/     │
│  8765/stats   │                 │  MEM/DISK/NET/     │
│               │                 │  BAT every 1 sec   │
└──────────────┘                 └──────────────────┘
```

## Project Structure

```
StateLink/
├── desktop/                        # PC-side monitor server
│   ├── statelink_server.py         # Python HTTP server + psutil stats collector
│   └── requirements.txt            # psutil, pyinstaller
├── android/StateLinkApp/           # Android app (open in Android Studio)
│   ├── app/src/main/
│   │   ├── java/com/statelink/app/
│   │   │   ├── MainActivity.java   # UI, polling, WakeLock, theme switching
│   │   │   └── ThemeManager.java   # Persist theme choice, map index→style res ID
│   │   └── res/
│   │       ├── layout/activity_main.xml   # Card-based stat display layout
│   │       ├── values/colors.xml          # Raw color hex values
│   │       ├── values/strings.xml         # All user-facing strings
│   │       ├── values/styles.xml          # 3 themes (Light, Dark, Tech Blue)
│   │       ├── drawable/dot_green.xml     # Connected indicator
│   │       ├── drawable/dot_red.xml       # Disconnected indicator
│   │       └── drawable/ic_launcher_*.xml # App icon
│   └── build.gradle               # AGP 8.2.0, minSdk 24, targetSdk 34
└── CLAUDE.md
```

## Key Technical Details

- **Communication**: The Python server runs an HTTP server on `0.0.0.0:8765`. It runs `adb reverse tcp:8765 tcp:8765` so the phone's `localhost:8765` maps to the PC's port 8765. The Android app polls `http://localhost:8765/stats` every 1 second via `HttpURLConnection` on a single-thread executor.
- **Screen always-on**: Two mechanisms — `FLAG_KEEP_SCREEN_ON` window flag + `SCREEN_BRIGHT_WAKE_LOCK` from PowerManager. Both released in `onDestroy()`.
- **Theme system**: 3 themes defined as Material3 styles in `styles.xml`. `ThemeManager` persists the choice to `SharedPreferences`. Switching calls `recreate()` to re-apply the style before `setContentView()`.
- **Stats JSON schema**: See the `collect_stats_loop()` function in `statelink_server.py`. Battery field is `-1` when unavailable (desktops); the app hides the battery card in that case.
- **Network speed**: Calculated as delta between `psutil.net_io_counters()` calls divided by elapsed time → KB/s.

## Development Commands

### Desktop server
```bash
cd desktop
pip install psutil                    # one-time dependency
python statelink_server.py            # run the server
```

Build standalone executables:
```bash
pip install pyinstaller
pyinstaller --onefile --console statelink_server.py   # → dist/statelink_server (or .exe)
```

### Android app
Open `android/StateLinkApp/` in Android Studio. Build with:
- **Run**: Click the green ▶ Run button (or Ctrl+R / Cmd+R)
- **Build APK**: Build → Build Bundle(s) / APK(s) → Build APK(s)
- **Gradle sync**: File → Sync Project with Gradle Files

The app requires:
- compileSdk 34, minSdk 24 (Android 7.0), targetSdk 34
- AGP 8.2.0, Gradle 8.4
- Dependencies: AppCompat 1.6.1, Material 1.11.0, CardView 1.0.0, ConstraintLayout 2.1.4
