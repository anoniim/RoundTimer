Round Timer - KMP Project
==

A board game round timer application designed to help players time their rounds with configurable durations, audio cues, and session tracking capabilities.

## Main Features
* Configurable round duration
* Visual and audio countdown feedback with increasing urgency
* Overtime tracking when rounds exceed configured time
* "Games" to group rounds and track history separately
* Round history with statistics for each game
* Customizable audio cues
* Clean, modern interface

## User Flows
1.  **Setup & Start:** Configure the round duration and select a game, then start the timer.
2.  **Active Round:** The timer counts down with visual and audio cues, and tracks overtime if needed.
3.  **History:** Review past rounds and statistics for each game.
4.  **Game Management:** Create, rename, and switch between different games.
5.  **Settings:** Customize audio cues and other features.

## Technical Stack

*   Language: Kotlin
*   Framework: Kotlin Multiplatform
*   UI: Compose Multiplatform
*   State Management: Kotlin Coroutines & StateFlow
*   Persistence: Platform-specific storage (`SharedPreferences` on Android, `NSUserDefaults` on iOS)
*   Targets: Android, iOS, Desktop (JVM), Web (Wasm)

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

### Build and Run

To build and run the development version of the app, use the run configuration from the run widget in your IDE’s toolbar or build it directly from the terminal.
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

### Build and Run Web Application

To build and run the development version of the web app, use the run configuration from the run widget
in your IDE's toolbar or run it directly from the terminal:
- for the Wasm target (faster, modern browsers):
  - on macOS/Linux
    ```shell
    ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
    ```
  - on Windows
    ```shell
    .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
    ```
- for the JS target (slower, supports older browsers):
  - on macOS/Linux
    ```shell
    ./gradlew :composeApp:jsBrowserDevelopmentRun
    ```
  - on Windows
    ```shell
    .\gradlew.bat :composeApp:jsBrowserDevelopmentRun
    ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### How to run

#### Desktop
./gradlew :composeApp:run

#### Android
./gradlew :composeApp:assembleDebug

#### Web (WASM)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
