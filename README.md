# Dhamma Player - Android

A native Android audio player for Dhamma talks from [DhammaTalks.org](https://www.dhammatalks.org/).

## Features

- **Library**: Browse and play evening talks from the DhammaTalks.org RSS feed
- **Offline Downloads**: Download tracks for offline listening
- **Progress Tracking**: Automatically saves and resumes playback position
- **Scheduled Playback**: Set alarms to automatically start playing at specified times
- **Background Playback**: Continue listening with the screen off via media notification

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM with Hilt dependency injection
- **Media**: Jetpack Media3 (ExoPlayer) with MediaSessionService
- **Database**: Room
- **Networking**: OkHttp + Retrofit

## Requirements

- Android 8.0 (API 26) or higher
- Internet connection for streaming/downloading

## Building

1. Open the `android` folder in Android Studio
2. Sync Gradle files
3. Run on device or emulator

```bash
# Or build from command line
./gradlew assembleDebug
```

## Permissions

- `INTERNET` - Fetch RSS feed and stream/download audio
- `POST_NOTIFICATIONS` - Show media playback notification (Android 13+)
- `FOREGROUND_SERVICE` - Background audio playback
- `SCHEDULE_EXACT_ALARM` - Scheduled playback feature
- `RECEIVE_BOOT_COMPLETED` - Re-register alarms after device reboot

## Project Structure

```
app/src/main/java/com/dhammaplayer/
├── DhammaPlayerApp.kt          # Hilt Application
├── MainActivity.kt             # Single Activity host
├── data/
│   ├── local/                  # Room database & DAOs
│   ├── model/                  # Data classes
│   ├── remote/                 # RSS feed service
│   └── repository/             # Data repositories
├── di/                         # Hilt modules
├── media/                      # Media3 playback service
├── receiver/                   # Broadcast receivers
├── ui/
│   ├── components/             # Reusable Composables
│   ├── navigation/             # Navigation host
│   ├── screens/                # Screen Composables
│   └── theme/                  # Colors & Theme
└── viewmodel/                  # ViewModels
```

## License

This app is for personal use to listen to freely available Dhamma talks.

