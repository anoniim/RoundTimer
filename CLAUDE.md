# CLAUDE.md - AI Assistant Guide for RoundTimer

> **Last Updated**: 2025-11-19
> **Project**: RoundTimer - Kotlin Multiplatform Round Timer Application
> **Package**: `net.solvetheriddle.roundtimer`

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Codebase Structure](#codebase-structure)
3. [Architecture & Design Patterns](#architecture--design-patterns)
4. [Key Conventions](#key-conventions)
5. [Platform-Specific Development](#platform-specific-development)
6. [Development Workflows](#development-workflows)
7. [Testing Guidelines](#testing-guidelines)
8. [Build & Deployment](#build--deployment)
9. [Code Modification Guidelines](#code-modification-guidelines)
10. [Common Tasks](#common-tasks)

---

## Project Overview

### What is RoundTimer?

RoundTimer is a **Kotlin Multiplatform** board game timer application that helps players manage round timing with configurable durations, audio cues, and session tracking. The app supports **5 platforms** from a single Kotlin codebase:

- **Android** (Primary target, published to Play Store)
- **iOS** (SwiftUI wrapper)
- **Desktop/JVM** (Cross-platform desktop app)
- **Web/WASM** (WebAssembly, modern browsers)
- **Web/JS** (JavaScript, legacy browser support)

### Core Features

- Configurable round duration with visual dial interface
- Real-time countdown with color-coded urgency (Green → Orange → Red)
- Overtime tracking with escalating audio alerts
- "Games" to organize and track rounds separately
- Round history with statistics per game
- Customizable audio cues (15 WAV files, ~11MB total)
- Material 3 design system
- Offline-first with platform-native persistence

### Technical Stack Summary

| Layer | Technology |
|-------|------------|
| **Language** | Kotlin 2.2.20 |
| **UI Framework** | Compose Multiplatform 1.9.0 |
| **State Management** | Kotlin Coroutines & StateFlow |
| **Navigation** | Custom sealed class routes |
| **Serialization** | kotlinx.serialization 1.9.0 |
| **Date/Time** | kotlinx-datetime 0.7.1 |
| **Android Persistence** | DataStore Preferences 1.1.1 |
| **iOS Persistence** | NSUserDefaults |
| **JVM Persistence** | Properties file |
| **Web Persistence** | localStorage |
| **Analytics** | Firebase Analytics (Android only) |
| **Build System** | Gradle 8.11.2 with Kotlin DSL |
| **Version Control** | Git-based versioning |

### Code Sharing Statistics

- **Total Kotlin files**: 81
- **Shared code**: ~85-90% (UI + business logic)
- **Platform-specific**: 10-15% (audio, storage, platform services)
- **Lines of code**: ~10,000+

---

## Codebase Structure

### Directory Layout

```
RoundTimer/
├── .github/workflows/           # CI/CD pipelines
│   ├── build-android.yml       # Android build & Play Store publishing
│   └── tag-release.yml         # Git tag creation workflow
├── buildSrc/                   # Custom Gradle build logic
│   └── src/main/kotlin/
│       └── GitVersioning.kt    # Automated versioning from Git
├── composeApp/                 # Main multiplatform module
│   ├── src/
│   │   ├── commonMain/         # Shared code (all platforms)
│   │   ├── androidMain/        # Android-specific implementations
│   │   ├── iosMain/            # iOS-specific implementations
│   │   ├── jvmMain/            # Desktop-specific implementations
│   │   ├── wasmJsMain/         # WASM/JS-specific implementations
│   │   ├── jsMain/             # Legacy JS-specific implementations
│   │   └── commonTest/         # Shared unit tests
│   └── build.gradle.kts        # Module build configuration
├── iosApp/                     # iOS native app wrapper
│   └── iosApp/                 # SwiftUI entry point
├── gradle/                     # Gradle wrapper & version catalog
│   └── libs.versions.toml      # Centralized dependency versions
├── build.gradle.kts            # Root build configuration
├── settings.gradle.kts         # Project settings
├── gradle.properties           # Gradle JVM & build settings
└── releaseNotes               # Play Store release notes
```

### Common Main Structure (Shared Code)

**Location**: `/composeApp/src/commonMain/kotlin/net/solvetheriddle/roundtimer/`

```
commonMain/
├── audio/
│   └── AudioScheduler.kt              # Millisecond-precision audio timing
├── model/
│   ├── AudioCue.kt                    # Audio event definitions & patterns
│   ├── Game.kt                        # Game session data model
│   ├── Round.kt                       # Individual round data model
│   ├── SettingsState.kt               # User preferences model
│   └── TimerState.kt                  # Main application state
├── platform/                          # Platform abstraction interfaces (expect)
│   ├── AnalyticsService.kt            # Analytics tracking interface
│   ├── DeviceUtils.kt                 # Platform info & capabilities
│   ├── ScreenLocker.kt                # Keep screen awake interface
│   ├── SoundPlayer.kt                 # Audio playback interface
│   └── StatusBarManager.kt            # System UI customization
├── storage/                           # Data persistence layer
│   ├── PlatformStorage.kt             # Platform-specific storage interface
│   ├── StorageFactory.kt              # Platform-specific storage creation
│   └── StorageManager.kt              # Business logic for data persistence
├── ui/
│   ├── components/                    # Reusable UI components
│   │   ├── ScrollableDial.kt          # Time selection dial
│   │   ├── SetAppropriateStatusBarColor.kt
│   │   └── StyledCard.kt              # Material card wrapper
│   ├── screens/                       # Main application screens (5 screens)
│   │   ├── ActiveTimerScreen.kt       # Running timer (177 lines)
│   │   ├── ConfigurationScreen.kt     # Timer setup (180 lines)
│   │   ├── GamesScreen.kt             # Game management (427 lines)
│   │   ├── HistoryScreen.kt           # Round history (465 lines)
│   │   └── SettingsScreen.kt          # App settings (157 lines)
│   ├── theme/
│   │   └── Theme.kt                   # Material 3 color schemes
│   └── utils/
│       └── WindowSize.kt              # Responsive layout utilities
├── viewmodel/
│   └── TimerViewModel.kt              # Main business logic (509 lines)
├── App.kt                             # Root composable & navigation
└── BackPressHandler.kt                # Platform back button handling
```

### Platform-Specific Directories

Each platform implements the `expect` declarations from `commonMain`:

**Android** (`androidMain/`):
- Uses SoundPool + MediaPlayer for audio
- DataStore Preferences for storage
- Firebase Analytics integration
- WindowManager for screen wake lock

**iOS** (`iosMain/`):
- AVAudioPlayer for audio
- NSUserDefaults for storage
- UIApplication.isIdleTimerDisabled for wake lock

**JVM** (`jvmMain/`):
- Java Clip for audio
- File-based Properties storage
- No analytics or wake lock

**WASM/JS** (`wasmJsMain/`):
- HTML5 Audio API
- Browser localStorage
- Screen Wake Lock API

### Resources

**Audio Files** (`/composeApp/src/commonMain/composeResources/files/`):
- `call.wav` - Subtle drumming cue
- `intense.wav` - 20-second intense drumming
- `timeout_gong.wav` - Timer completion sound
- `overtime_beat_alarm.wav` - Overtime alert
- 6 overtime voice cues (Jonas scolding feature)
- **Total**: 15 WAV files, ~11MB

**Drawables** (`/composeApp/src/commonMain/composeResources/drawable/`):
- Vector graphics and icons

---

## Architecture & Design Patterns

### Architectural Pattern: MVI (Model-View-Intent)

The app follows **Model-View-Intent** architecture with **unidirectional data flow**:

```
User Interaction (Intent)
    ↓
TimerViewModel (Intent Handler)
    ↓
State Update (Model)
    ↓
StateFlow Emission
    ↓
UI Recomposition (View)
    ↓
[Loop back to User Interaction]
```

### State Management

**Single Source of Truth**:
- `TimerState` data class holds all application state
- `MutableStateFlow<TimerState>` in `TimerViewModel`
- Immutable state updates (copy with modifications)
- Reactive UI updates via `collectAsState()`

**TimerState Structure** (`model/TimerState.kt:37`):
```kotlin
@Serializable
data class TimerState(
    val configuredTime: Long,      // Target round duration
    val currentTime: Long,          // Countdown value
    val overtimeTime: Long,         // Time past configured
    val isRunning: Boolean,         // Timer active state
    val isOvertime: Boolean,        // Past configured time
    val startTimestamp: Long?,      // Timer start time
    val rounds: List<Round>,        // Historical rounds
    val games: List<Game>,          // Game sessions
    val activeGameId: String?,      // Current game
    val settings: SettingsState     // User preferences
)
```

### ViewModel Pattern

**TimerViewModel** (`viewmodel/TimerViewModel.kt`) is the **single ViewModel** managing:
- Timer countdown logic
- Audio scheduling coordination
- Data persistence (auto-save on state changes)
- Analytics event tracking
- Game/round CRUD operations
- Settings management

**Key Methods**:
- `startTimer()` - Initialize countdown with audio scheduling
- `stopTimer()` - Halt countdown, save round
- `resetTimer()` - Clear state to configured time
- `onTimerTick()` - Update state every 50ms
- `createGame()` / `deleteGame()` / `renameGame()` - Game management
- `saveState()` - Persist to platform storage

### Navigation

**Route-Based Navigation** (`App.kt:24`):
```kotlin
sealed class Screen {
    data object Configuration : Screen()
    data object ActiveTimer : Screen()
    data object History : Screen()
    data object Games : Screen()
    data object Settings : Screen()
}
```

- State-driven: `currentScreen: MutableState<Screen>`
- Transitions: `Crossfade` animations
- No external navigation library

### Separation of Concerns

| Layer | Responsibility | Files |
|-------|----------------|-------|
| **Models** | Data structures, no logic | `model/*.kt` |
| **ViewModels** | Business logic, state management | `viewmodel/TimerViewModel.kt` |
| **Views** | UI presentation only | `ui/screens/*.kt` |
| **Platform** | OS-specific implementations | `platform/*.kt` (expect/actual) |
| **Storage** | Data persistence abstraction | `storage/*.kt` |
| **Audio** | Audio timing & playback | `audio/*.kt` + `platform/SoundPlayer.kt` |

### Design Patterns Used

1. **Expect/Actual** - Platform abstraction for native APIs
2. **Singleton** - Platform service instances (`getSoundPlayer()`)
3. **Factory** - Platform-specific object creation (`StorageFactory`)
4. **Observer** - StateFlow for reactive updates
5. **Strategy** - Audio patterns (Single, Repeated, Custom)
6. **Repository** - `StorageManager` abstracts persistence details

### Dependency Injection

**Manual DI** (no framework like Koin/Dagger):
- Factory functions: `getSoundPlayer()`, `getAnalyticsService()`
- Lazy initialization in ViewModel constructor
- Singleton pattern for platform services
- Context/Platform passed explicitly where needed

---

## Key Conventions

### Code Style

**Kotlin Official Style** (`gradle.properties:2`):
```properties
kotlin.code.style=official
```

**Naming Conventions**:
- **Classes**: PascalCase (`TimerViewModel`, `AudioScheduler`)
- **Functions**: camelCase (`startTimer()`, `onTimerTick()`)
- **Properties**: camelCase (`isRunning`, `currentTime`)
- **Constants**: SCREAMING_SNAKE_CASE (`MAX_DURATION`, `AUDIO_INTERVAL`)
- **Composables**: PascalCase (`ActiveTimerScreen`, `StyledCard`)
- **Files**: Match primary class name (`TimerViewModel.kt`)

**Formatting**:
- Indentation: 4 spaces (no tabs)
- Line length: No strict limit (use judgment)
- Imports: Alphabetical, remove unused
- Trailing commas: Use in multiline lists

### File Organization

**Package Structure**:
```
net.solvetheriddle.roundtimer.{layer}
```

**Layer Naming**:
- `model` - Data classes
- `viewmodel` - Business logic
- `ui.screens` - Full-screen composables
- `ui.components` - Reusable UI elements
- `ui.theme` - Theming & colors
- `platform` - Platform abstractions
- `storage` - Persistence layer
- `audio` - Audio timing logic

**File Naming**:
- One primary class per file
- File name matches class name
- Exceptions: Extension functions, utilities

### State Immutability

**Always use immutable data classes**:
```kotlin
// CORRECT
_state.value = _state.value.copy(currentTime = newTime)

// WRONG
_state.value.currentTime = newTime  // Won't compile (val property)
```

### Expect/Actual Pattern

**Common (expect)**:
```kotlin
expect class SoundPlayer {
    fun play(soundId: String)
    fun release()
}
```

**Platform (actual)**:
```kotlin
actual class SoundPlayer(context: Context) {
    actual fun play(soundId: String) { /* Android implementation */ }
    actual fun release() { /* Cleanup */ }
}
```

**Rules**:
- Keep `expect` declarations minimal (interface-like)
- Implement all `actual` declarations on all platforms
- Use factory functions for platform-specific constructors

### Serialization

**Use kotlinx.serialization**:
```kotlin
@Serializable
data class Game(
    val id: String,
    val date: String,
    val name: String = ""
)
```

**JSON encoding** (`storage/StorageManager.kt:16`):
```kotlin
private val json = Json {
    prettyPrint = false
    ignoreUnknownKeys = true
}
```

### Composable Conventions

**Stateless Composables**:
```kotlin
@Composable
fun ActiveTimerScreen(
    state: TimerState,
    onStop: () -> Unit,
    onReset: () -> Unit
)
```

**State Hoisting**:
- Screens receive state and callbacks
- ViewModel manages state
- Screens don't directly modify state

**Preview Annotations**:
```kotlin
@Preview
@Composable
fun ActiveTimerScreenPreview() {
    // Preview with sample data
}
```

### Error Handling

**Graceful Degradation**:
- Use `try-catch` for platform-specific code
- Provide fallback values
- Log errors (avoid silent failures)

**Example** (`storage/PlatformStorage.android.kt:24`):
```kotlin
override suspend fun saveString(key: String, value: String) {
    try {
        withTimeout(5000) {
            dataStore.edit { it[stringPreferencesKey(key)] = value }
        }
    } catch (e: Exception) {
        println("Storage error: ${e.message}")
    }
}
```

### Analytics

**Track meaningful events** (`viewmodel/TimerViewModel.kt:90`):
```kotlin
analytics.logEvent("timer_started", mapOf(
    "configured_time" to configuredTime,
    "game_id" to activeGameId
))
```

**Event Naming**:
- Use snake_case
- Be descriptive but concise
- Include relevant parameters

---

## Platform-Specific Development

### Adding Platform-Specific Features

**1. Define expect declaration** (`commonMain/platform/NewFeature.kt`):
```kotlin
expect class NewFeature {
    fun doSomething(): String
}
```

**2. Implement actual for each platform**:
- `androidMain/platform/NewFeature.android.kt`
- `iosMain/platform/NewFeature.ios.kt`
- `jvmMain/platform/NewFeature.jvm.kt`
- `wasmJsMain/platform/NewFeature.wasmJs.kt`

**3. Use factory pattern for initialization**:
```kotlin
expect fun getNewFeature(): NewFeature
```

### Android-Specific Notes

**Minimum SDK**: 24 (Android 7.0)
**Target SDK**: 36 (Android 15)
**Compile SDK**: 36

**Key Libraries**:
- `androidx.datastore:datastore-preferences:1.1.1`
- `androidx.core:core-splashscreen:1.0.1`
- `com.google.firebase:firebase-analytics` (via BOM 34.3.0)

**Entry Point**: `androidMain/MainActivity.kt`

**ProGuard**: Enabled for release builds

**Signing**:
- Keystore from GitHub Secrets (`ANDROID_KEYSTORE`)
- Password from env var (`ANDROID_KEYSTORE_PASS`) or `local.properties`

### iOS-Specific Notes

**Minimum iOS**: Not explicitly set (Kotlin default: iOS 14.0)
**Framework**: Static framework linking AVFoundation

**Entry Point**: `iosApp/iosApp/iOSApp.swift`

**Audio Session**:
```kotlin
AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, null)
```

**Build**: Open `iosApp/` in Xcode, build/run from there

### Desktop/JVM Notes

**JVM Target**: 11

**Storage Location**: `~/.roundtimer/storage.properties`

**Run Command**:
```bash
./gradlew :composeApp:run
```

**Distribution Formats**: DMG (macOS), MSI (Windows), DEB (Linux)

### Web (WASM/JS) Notes

**WASM Target** (Recommended):
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

**JS Target** (Legacy):
```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
```

**Storage**: Browser localStorage API

**Screen Wake**: Screen Wake Lock API (WASM/JS shared implementation)

---

## Development Workflows

### Local Development Setup

**Prerequisites**:
- JDK 17 (for Android builds)
- JDK 11+ (for desktop development)
- Android Studio (recommended IDE)
- Xcode (for iOS builds, macOS only)
- Git

**Clone & Build**:
```bash
git clone <repository-url>
cd RoundTimer
./gradlew build
```

**Run Configurations**:
| Platform | Command |
|----------|---------|
| Android | `./gradlew :composeApp:assembleDebug` |
| iOS | Open `iosApp/` in Xcode |
| Desktop | `./gradlew :composeApp:run` |
| WASM | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` |
| JS | `./gradlew :composeApp:jsBrowserDevelopmentRun` |

### Gradle Configuration

**JVM Memory** (`gradle.properties:6`):
```properties
org.gradle.jvmargs=-Xmx4096M -Dfile.encoding=UTF-8
kotlin.daemon.jvmargs=-Xmx4096M
```

**Performance Optimizations**:
```properties
org.gradle.configuration-cache=true
org.gradle.caching=true
```

**Android**:
```properties
android.nonTransitiveRClass=true
android.useAndroidX=true
```

### Git Workflow

**Branching Strategy**:
- `main` - Production-ready code
- Feature branches: `feature/name-description`
- Claude branches: `claude/claude-md-*` (for AI assistant work)

**Versioning** (Git-based):
- **Version Code**: Total commit count on current branch
- **Version Name**: Latest Git tag or "snapshot"

**Creating Releases**:
1. Trigger `.github/workflows/tag-release.yml` workflow
2. Provide version name (e.g., `v1.2.3`)
3. Provide release notes
4. Workflow creates Git tag and pushes

### CI/CD Pipeline

**Build Workflow** (`.github/workflows/build-android.yml`):

**Triggers**:
- Push to `main` branch
- Git tag creation

**Steps**:
1. Checkout with full history (`fetch-depth: 0`)
2. Set up JDK 17 with Gradle cache
3. Load keystore from GitHub Secrets
4. Build release bundle (`./gradlew composeApp:bundleRelease`)
5. Upload AAB artifact (14 days for `main`, 90 days for tags)
6. Publish to Play Store:
   - `main` push → **internal** track
   - Tag creation → **alpha** track

**Required Secrets**:
- `ANDROID_KEYSTORE` (base64-encoded keystore file)
- `ANDROID_KEYSTORE_PASS` (keystore password)
- `PLAY_STORE_SERVICE_ACCOUNT_JSON` (Google Play service account)

### Local Development Tips

**Hot Reload**: Enabled via `compose-hot-reload` plugin (beta)

**Incremental Compilation**: Enabled by default

**Build Cache**: Enabled in `gradle.properties`

**Clean Build**:
```bash
./gradlew clean build
```

**Check for Updates**:
```bash
./gradlew dependencyUpdates
```

---

## Testing Guidelines

### Test Structure

**Location**: `/composeApp/src/commonTest/kotlin/net/solvetheriddle/roundtimer/`

**Framework**: `kotlin-test` multiplatform

**Current State**: Minimal test coverage (1 test file)

### Writing Tests

**Example Test**:
```kotlin
@Test
fun testTimerStateInitialization() {
    val state = TimerState(
        configuredTime = 60000L,
        currentTime = 60000L,
        overtimeTime = 0L,
        isRunning = false,
        isOvertime = false,
        startTimestamp = null,
        rounds = emptyList(),
        games = emptyList(),
        activeGameId = null,
        settings = SettingsState()
    )
    assertEquals(60000L, state.configuredTime)
    assertFalse(state.isRunning)
}
```

### Test Coverage Priorities

**High Priority**:
1. `TimerViewModel` business logic
2. `AudioScheduler` timing calculations
3. `StorageManager` serialization/deserialization
4. State transformations in ViewModel

**Medium Priority**:
1. Round/Game CRUD operations
2. Settings management
3. Overtime calculations

**Low Priority**:
1. UI tests (Compose testing)
2. Platform-specific implementations (manual testing)

### Running Tests

**Common Tests** (all platforms):
```bash
./gradlew :composeApp:commonTest
```

**Android Tests**:
```bash
./gradlew :composeApp:testDebugUnitTest
```

**JVM Tests**:
```bash
./gradlew :composeApp:jvmTest
```

---

## Build & Deployment

### Building for Production

**Android Release Bundle**:
```bash
./gradlew :composeApp:bundleRelease
```
Output: `composeApp/build/outputs/bundle/release/composeApp-release.aab`

**Android Debug APK**:
```bash
./gradlew :composeApp:assembleDebug
```
Output: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`

**Desktop Distributions**:
```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```
Output: `composeApp/build/compose/binaries/main/[dmg|msi|deb]/`

**iOS Framework**:
```bash
./gradlew :composeApp:linkReleaseFrameworkIosArm64
```

### Version Management

**Automated Versioning** (`buildSrc/src/main/kotlin/GitVersioning.kt`):

**Version Code**:
```kotlin
fun getGitVersionCode(): Int {
    return exec("git rev-list --count HEAD").toInt()
}
```

**Version Name**:
```kotlin
fun getGitVersionName(): String {
    val tag = exec("git describe --tags --always")
    return tag.ifEmpty { "snapshot" }
}
```

**Usage in build**:
```kotlin
android {
    defaultConfig {
        versionCode = getGitVersionCode()
        versionName = getGitVersionName()
    }
}
```

### Release Process

**Manual Release** (via GitHub Actions):
1. Go to GitHub Actions
2. Select "Create new release" workflow
3. Click "Run workflow"
4. Enter version name (e.g., `v1.2.3`)
5. Enter release notes
6. Workflow creates tag and triggers build

**Automated Publishing**:
- Tag creation triggers Android build
- Build uploads to Play Store **alpha** track
- Manual promotion from alpha → beta → production

### Release Notes

**Location**: `/releaseNotes` (plain text file)

**Format**:
```
- Feature: Added new game statistics
- Fix: Resolved audio timing issues
- Improvement: Better UI responsiveness
```

**Note**: Release notes upload to Play Store currently has a bug in the publishing plugin.

---

## Code Modification Guidelines

### Before Making Changes

**1. Understand Context**:
- Read related files (models, viewmodel, screens)
- Check if similar patterns exist elsewhere
- Review platform-specific implications

**2. Check Dependencies**:
- Is this shared code or platform-specific?
- Will changes affect all platforms?
- Are there platform-specific workarounds?

**3. Consider State**:
- How does this affect `TimerState`?
- Is state update immutable?
- Will this trigger UI recomposition?

### Adding New Features

**1. Add to Data Model** (if needed):
```kotlin
// model/TimerState.kt
@Serializable
data class TimerState(
    // ... existing properties
    val newFeature: Boolean = false  // Add with default
)
```

**2. Update ViewModel**:
```kotlin
// viewmodel/TimerViewModel.kt
fun enableNewFeature() {
    _state.value = _state.value.copy(newFeature = true)
    saveState()  // Persist
    analytics.logEvent("new_feature_enabled")
}
```

**3. Update UI**:
```kotlin
// ui/screens/SettingsScreen.kt
Switch(
    checked = state.newFeature,
    onCheckedChange = { viewModel.enableNewFeature() }
)
```

**4. Update Storage** (if state structure changed):
```kotlin
// storage/StorageManager.kt
// Increment STORAGE_VERSION if incompatible
private const val STORAGE_VERSION = "2.0"
```

### Modifying Audio System

**Audio Timing**: Handled by `AudioScheduler` (162 lines)
- Pre-calculates all audio events at timer start
- Uses monotonic time source (millisecond precision)
- Separated from UI updates (50ms UI, 1ms audio)

**Adding New Audio Cue**:

1. Add audio file to `/composeApp/src/commonMain/composeResources/files/`
2. Update `AudioCue.kt` with new sound ID
3. Update platform `SoundPlayer` implementations to load new file
4. Update `AudioScheduler` logic to schedule new cue

**Example**:
```kotlin
// model/AudioCue.kt
object AudioFiles {
    const val NEW_SOUND = "new_sound.wav"
}

// viewmodel/TimerViewModel.kt
if (remainingTime == 30000L) {
    audioScheduler.scheduleAudioEvent(
        AudioCue.Single(AudioFiles.NEW_SOUND, 30000L)
    )
}
```

### Modifying Storage

**Key-Value Storage**:
- `PlatformStorage` interface defines CRUD operations
- `StorageManager` handles serialization with `kotlinx.serialization.json`
- All state changes trigger auto-save via ViewModel

**Adding New Stored Property**:

1. Add to relevant model (e.g., `SettingsState`)
2. Update serialization (automatic with `@Serializable`)
3. Provide default value for backward compatibility
4. Increment `STORAGE_VERSION` if incompatible

**Storage Keys** (`storage/StorageManager.kt:11-16`):
```kotlin
private const val KEY_CONFIGURED_TIME = "configured_time"
private const val KEY_ROUNDS = "rounds"
private const val KEY_GAMES = "games"
private const val KEY_ACTIVE_GAME_ID = "active_game_id"
private const val KEY_SETTINGS = "settings"
private const val KEY_STORAGE_VERSION = "storage_version"
```

### Adding New Screens

**1. Create Screen Composable**:
```kotlin
// ui/screens/NewScreen.kt
@Composable
fun NewScreen(
    state: TimerState,
    onNavigateBack: () -> Unit
) {
    // Screen UI
}
```

**2. Add Route to Sealed Class**:
```kotlin
// App.kt
sealed class Screen {
    // ... existing routes
    data object NewScreen : Screen()
}
```

**3. Add to Navigation**:
```kotlin
// App.kt
when (currentScreen.value) {
    // ... existing screens
    Screen.NewScreen -> NewScreen(
        state = state,
        onNavigateBack = { currentScreen.value = Screen.Configuration }
    )
}
```

**4. Add Navigation Trigger**:
```kotlin
// From another screen
IconButton(onClick = { currentScreen.value = Screen.NewScreen }) {
    Icon(Icons.Default.NewIcon, "Navigate to new screen")
}
```

### Styling Guidelines

**Material 3 Theme** (`ui/theme/Theme.kt:13`):
```kotlin
MaterialTheme(
    colorScheme = if (isDarkTheme) darkScheme else lightScheme
)
```

**Color Palette**:
- Uses Material 3 dynamic color generation
- Light and dark schemes defined
- Primary, secondary, tertiary colors

**Typography**: Default Material 3 type scale

**Custom Colors**:
```kotlin
val CustomGreen = Color(0xFF4CAF50)
val CustomOrange = Color(0xFFFF9800)
val CustomRed = Color(0xFFF44336)
```

### Performance Considerations

**Timer Updates**: 50ms interval (20 FPS)
```kotlin
// viewmodel/TimerViewModel.kt
viewModelScope.launch {
    while (isActive) {
        delay(50)
        onTimerTick()
    }
}
```

**Audio Precision**: 1ms check interval in `AudioScheduler`

**State Flow**: Only emit when state actually changes
```kotlin
_state.value = newState  // Only if different
```

**Compose Recomposition**:
- Hoist state to minimize recomposition scope
- Use `remember` for expensive calculations
- Use `derivedStateOf` for computed properties

---

## Common Tasks

### Task: Add a New Setting

**Example**: Add "Vibration on Overtime" setting

**1. Update Model**:
```kotlin
// model/SettingsState.kt
@Serializable
data class SettingsState(
    // ... existing settings
    val isVibrationEnabled: Boolean = false
)
```

**2. Update ViewModel**:
```kotlin
// viewmodel/TimerViewModel.kt
fun toggleVibration() {
    _state.value = _state.value.copy(
        settings = _state.value.settings.copy(
            isVibrationEnabled = !_state.value.settings.isVibrationEnabled
        )
    )
    saveState()
}
```

**3. Update Settings Screen**:
```kotlin
// ui/screens/SettingsScreen.kt
Row {
    Text("Vibration on Overtime")
    Switch(
        checked = state.settings.isVibrationEnabled,
        onCheckedChange = { viewModel.toggleVibration() }
    )
}
```

**4. Implement Platform-Specific Vibration**:
```kotlin
// platform/Vibrator.kt (expect)
expect class Vibrator {
    fun vibrate(durationMs: Long)
}

// platform/Vibrator.android.kt (actual)
actual class Vibrator(context: Context) {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator

    actual fun vibrate(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(durationMs)
        }
    }
}

// iOS, JVM, WASM implementations (may be no-op)
```

**5. Use in ViewModel**:
```kotlin
// viewmodel/TimerViewModel.kt
private val vibrator = getVibrator()

private fun onOvertimeStart() {
    if (_state.value.settings.isVibrationEnabled) {
        vibrator.vibrate(500)
    }
}
```

### Task: Update Dependencies

**1. Edit Version Catalog**:
```kotlin
// gradle/libs.versions.toml
[versions]
kotlin = "2.2.21"  # Updated version
```

**2. Sync Gradle**:
```bash
./gradlew --refresh-dependencies
```

**3. Test Build**:
```bash
./gradlew clean build
```

**4. Test on All Platforms**:
- Android: `./gradlew :composeApp:assembleDebug`
- Desktop: `./gradlew :composeApp:run`
- WASM: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`

### Task: Debug Audio Issues

**Check AudioScheduler**:
```kotlin
// Add logging in AudioScheduler.kt
println("Scheduling audio: $audioCue at ${audioCue.triggerTime}ms")
```

**Check SoundPlayer**:
```kotlin
// Add logging in platform SoundPlayer
println("Playing sound: $soundId")
```

**Verify Audio Files**: Ensure files exist in `composeResources/files/`

**Check Timing**: Use `TimeSource.Monotonic.markNow()` to verify precision

### Task: Add Analytics Event

**ViewModel** (`viewmodel/TimerViewModel.kt`):
```kotlin
fun onUserAction() {
    analytics.logEvent("user_action", mapOf(
        "parameter1" to "value1",
        "parameter2" to 123
    ))
}
```

**Event Naming**: Use snake_case, be descriptive

**Platform Support**: Currently only Android (Firebase), others no-op

### Task: Profile Performance

**Desktop**:
```bash
./gradlew :composeApp:run --profile
```
Profiling report: `build/reports/profile/`

**Android**:
- Use Android Studio Profiler
- Enable "Compose Layout Inspector"
- Monitor recompositions with Compose debugging

### Task: Refactor Code

**1. Extract Common Logic**:
```kotlin
// Before: Duplicated in multiple places
if (state.isRunning && !state.isOvertime) { /* ... */ }

// After: Extract to extension
fun TimerState.isActiveCountdown() = isRunning && !isOvertime
if (state.isActiveCountdown()) { /* ... */ }
```

**2. Split Large Files**:
- If a screen exceeds 500 lines, extract components
- Move reusable components to `ui/components/`

**3. Maintain Immutability**:
- Always use `.copy()` for state updates
- Never mutate collections directly

---

## Important Notes for AI Assistants

### When to Ask for Clarification

1. **Platform-specific changes**: "Should this apply to all platforms or specific ones?"
2. **Breaking changes**: "This will require storage migration. Proceed?"
3. **UI/UX decisions**: "Which color scheme for this new feature?"
4. **Analytics**: "What parameters should be logged for this event?"

### Common Pitfalls to Avoid

1. **Mutating state directly**: Always use `.copy()`
2. **Forgetting platform implementations**: Update all `actual` declarations
3. **Skipping storage version**: Increment if state structure changes incompatibly
4. **Breaking audio timing**: AudioScheduler is millisecond-precise, don't add delays
5. **Ignoring platform differences**: Android ≠ iOS ≠ JVM ≠ Web

### Best Practices

1. **Always read before write**: Check existing implementations first
2. **Follow existing patterns**: Match the codebase style
3. **Test on multiple platforms**: Changes may affect platforms differently
4. **Document platform-specific code**: Explain why something is platform-specific
5. **Preserve state immutability**: Core architectural principle

### Resources

**Official Documentation**:
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
- [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime)

**Project-Specific**:
- README.md - Project overview & build instructions
- TODO.md - Current tasks and planned features
- releaseNotes - Latest release information

---

## Appendix: Quick Reference

### File Locations Quick Reference

| What | Where |
|------|-------|
| Main business logic | `viewmodel/TimerViewModel.kt` |
| App state definition | `model/TimerState.kt` |
| Audio timing | `audio/AudioScheduler.kt` |
| Navigation | `App.kt` (Screen sealed class) |
| Theme colors | `ui/theme/Theme.kt` |
| Android entry point | `androidMain/MainActivity.kt` |
| iOS entry point | `iosApp/iosApp/iOSApp.swift` |
| Platform storage interface | `storage/PlatformStorage.kt` |
| Audio playback interface | `platform/SoundPlayer.kt` |
| Gradle dependencies | `gradle/libs.versions.toml` |
| Build configuration | `composeApp/build.gradle.kts` |
| CI/CD workflows | `.github/workflows/` |

### Key Numbers

- **Platforms**: 5 (Android, iOS, Desktop, WASM, JS)
- **Screens**: 5 (Configuration, ActiveTimer, History, Games, Settings)
- **Audio files**: 15 WAV files (~11MB)
- **Timer UI update**: 50ms (20 FPS)
- **Audio check interval**: 1ms (millisecond precision)
- **Min Android SDK**: 24
- **Target Android SDK**: 36
- **JVM Target**: 11 (Android), 11+ (Desktop)
- **Code sharing**: ~85-90%

### Gradle Commands Quick Reference

```bash
# Build all platforms
./gradlew build

# Run desktop
./gradlew :composeApp:run

# Build Android debug APK
./gradlew :composeApp:assembleDebug

# Build Android release bundle
./gradlew :composeApp:bundleRelease

# Run WASM dev server
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Run JS dev server
./gradlew :composeApp:jsBrowserDevelopmentRun

# Clean build
./gradlew clean

# Run tests
./gradlew test

# Check dependencies
./gradlew dependencies
```

---

**Document Version**: 1.0
**Last Updated**: 2025-11-19
**Maintained By**: AI assistants working on this codebase

For questions or updates to this document, modify `/CLAUDE.md` directly.
