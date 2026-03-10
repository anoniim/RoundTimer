# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

RoundTimer is a Kotlin Multiplatform (KMP) board game timer application targeting Android, iOS, Desktop (JVM), and Web (WASM) platforms. It uses Compose Multiplatform for the UI layer and follows a clean architecture pattern with ViewModel state management.

## Development Commands

### Build and Run
```bash
# Desktop (JVM) - Primary development target
./gradlew :composeApp:run

# Android debug build
./gradlew :composeApp:assembleDebug

# Web (WASM) - Modern browsers
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Web (JS) - Legacy browser support
./gradlew :composeApp:jsBrowserDevelopmentRun

# iOS - Open iosApp in Xcode or use IDE run configuration
```

### Testing
```bash
# Run common tests
./gradlew :composeApp:testCommonUnitTest

# Run all tests
./gradlew test
```

### Build Production
```bash
# Android release (requires signing config)
./gradlew :composeApp:assembleRelease

# Desktop distributables
./gradlew :composeApp:createDistributable
```

## Code Architecture

### Project Structure
- `/composeApp/src/commonMain/kotlin/` - Shared business logic and UI
- `/composeApp/src/androidMain/kotlin/` - Android-specific implementations  
- `/composeApp/src/jvmMain/kotlin/` - Desktop-specific implementations
- `/composeApp/src/iosMain/kotlin/` - iOS-specific implementations
- `/composeApp/src/wasmJsMain/kotlin/` - Web WASM implementations
- `/buildSrc/` - Custom Gradle build logic including Git versioning

### Core Components

**State Management**: Uses `TimerViewModel` with StateFlow for reactive state management. The app has a single source of truth in `TimerState` data class.

**Platform Abstraction**: Platform-specific functionality is abstracted through expect/actual declarations:
- `SoundPlayer` - Audio playback across platforms
- `ScreenLocker` - Prevents screen sleep during timer
- `PlatformStorage` - Data persistence (DataStore on Android, UserDefaults on iOS)
- `AnalyticsService` - Event tracking with Firebase on Android

**Navigation**: Simple screen-based navigation using sealed classes and Crossfade animations. No complex navigation library - just state-driven screen switching.

**Audio System**: Complex audio cue system with different drum sounds based on remaining time:
- 60s, 50s, 40s: Single call sound
- 36s, 19s: Intense sound  
- 0s and overtime: Overtime sound

**Data Models**:
- `TimerState` - Main app state container
- `Round` - Individual timer session record
- `Game` - Group of rounds with date and name
- `AudioCue` - Audio configuration for timer thresholds

### Key Files to Understand
- `App.kt` - Main app composition and screen routing
- `TimerViewModel.kt` - Core business logic and state management
- `TimerState.kt` - App state data model
- Platform-specific `SoundPlayer` implementations for audio handling
- Storage implementations for data persistence

### Development Patterns
- **Coroutines**: Heavy use of coroutines for timer operations and storage I/O
- **Compose**: Modern declarative UI with Material 3 design system
- **State Hoisting**: UI components receive state and callbacks, no local state
- **Platform Types**: Expect/actual pattern for platform-specific functionality
- **Error Handling**: Graceful degradation for storage and audio failures

### Build Configuration
- Uses Gradle version catalogs (`gradle/libs.versions.toml`)
- Custom Git versioning in `buildSrc/GitVersioning.kt`
- Firebase integration for Android analytics
- Hot reload enabled for development

### Testing Strategy
Currently minimal test coverage. When adding tests:
- Unit tests go in `commonTest` for shared logic
- Platform tests in respective `{platform}Test` directories
- Focus on `TimerViewModel` business logic and utility functions

### Platform-Specific Notes
- **Android**: Uses DataStore for persistence, Firebase Analytics, system dark mode detection
- **Desktop**: Custom macOS dark mode detection, Swing coroutines integration  
- **iOS**: AVFoundation framework linking for audio, UserDefaults storage
- **Web**: Limited audio capabilities, localStorage for persistence