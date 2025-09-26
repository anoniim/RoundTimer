Round Timer - KMP Project
===

A board game round timer application designed to help players time their rounds with configurable durations, audio cues, and session tracking capabilities.

## Main Features
* Configurable round duration (30 seconds to 10 minutes)
* Visual and audio countdown feedback with increasing urgency
* Overtime tracking when rounds exceed configured time
* Round history with statistics
* Progressive audio cues using djembe drum sounds
* Clean, modern interface with gradient animations

## User Flows
1. **Quick Timer Setup**  
   User opens the app → sees configuration screen  
   User adjusts slider to set desired round duration  
   User views formatted time display  
   User clicks "Start" → timer begins  
2. **Active Round Session**  
   Timer starts counting down  
   Visual feedback through:  
   Large countdown display  
   Progressive background fill  
   Color changes (green → yellow → orange → red)  
   Audio feedback at intervals:  
   30s: drum beat every 5 seconds  
   20s: drum beat every second  
   15s: double drum beat every second  
   10s: 2 double beats per second (4 beats total)  
   Timer reaches 0:00 → gong sound plays  
   Overtime tracking begins (if not stopped)  
   User clicks "Stop" → round saved to history  
3. **History Review**
   User clicks History icon (clock icon) in top-right  
   Sheet slides in from right with round history  
   User reviews:  
   Individual round details  
   Total statistics  
   Average round time  
   User can delete individual rounds or reset all history  
   Screen-by-Screen Breakdown  

## Configuration Screen (Initial State)
Purpose: Allow users to set up their round timer before starting  

### UI Components:

* Title: "Round Timer" with gradient text effect
* Time Display: Large centered time showing configured duration (e.g., "2:30")
* Duration Slider:
* Range: 30 seconds to 300 seconds (5 minutes)
* Step: 30-second increments
* Labels: "30 sec" (left) and "5 min" (right)
* Start Button: Full-width gradient button labeled "Start"
* History Icon: Clock icon button in top-right corner
* Background: Subtle gradient background 

### Functionality & Interactions:

* Slider Drag: Updates both configured time and display in real-time
* Start Button Click:
  * Transitions to Active Timer screen
  * Initializes countdown
  * Records start time
* History Icon Click: Opens History Sheet from right side

## Active Timer Screen
Purpose: Display active countdown with visual and audio feedback

### UI Components:

* Countdown Display: Extra-large timer (8xl font) showing remaining time
* Overtime Display: Red text showing overtime duration ("+0:15") when applicable
* Stop Button: Centered gradient button at bottom
* Progress Fill: Background color fill rising from bottom
* Pulse Animation: Card pulses during overtime

### Functionality & Interactions:

* Timer Countdown:
  * Decrements every second
  * No leading zeros (shows "2:30" not "02:30")
* Audio Cues:
  * Plays drum sounds at specified intervals
  * Gong sound at 0:00
* Visual Feedback:
  * Background fill percentage increases as time progresses
  * Color transitions: green → yellow-green → orange → red
  * Card pulses during overtime
* Stop Button Click:
  * Saves round to history
  * Returns to Configuration screen
  * Resets timer state

## History Sheet (Overlay)
Purpose: Review past rounds and statistics

### UI Components:

* Header: "Round History" title
* Round List: Scrollable list (max-height: 400px) containing:
  * Round number
  * Duration time
  * Overtime indicator (if applicable)
  * Delete icon for each round
* Statistics Section:
  * Total Rounds count
  * Total Time accumulated
  * Average Time per round
* Reset History Button: Destructive variant button
* Empty State: "No rounds recorded yet" message

### Functionality & Interactions:

* Delete Round:
  * Trash icon removes individual round
  * Updates statistics immediately
  * Persists to localStorage
* Reset History:
  * Clears all rounds
  * Resets statistics to zero
  * Updates localStorage
* Sheet Behavior:
  * Slides in from right
  * Click outside or X to close
  * Maintains state when closed

## Data Models

### Round Object

```
interface Round {
id: string;           // Unique identifier (timestamp-based)
duration: number;     // Actual round duration in seconds
overtime: number;     // Overtime seconds (0 if none)
timestamp: Date;      // When round was completed
}
```

### Timer State

```
interface TimerState {
configuredSeconds: number;  // User-selected duration (30-300)
currentSeconds: number;     // Current countdown value
overtimeSeconds: number;    // Overtime counter
isRunning: boolean;        // Timer active state
isOvertime: boolean;       // Overtime mode flag
rounds: Round[];           // Historical rounds array
}
```

### Audio Configuration

```
interface AudioCue {
threshold: number;         // Seconds remaining
beatPattern: 'single' | 'double';
}
```

## Technical Details

### Storage

localStorage Key: timerRounds
Data Format: JSON stringified array of Round objects
Persistence: Automatic on round completion and deletion

### Audio System
Technology: Web Audio API for synthesized sounds
Fallback: MP3 file for gong sound with synthesized backup
Context: Single AudioContext instance (lazy initialization)

### Visual Design
Color Scheme:
* Primary gradient for buttons and title
* Progressive colors for urgency (green to red)
* Muted backgrounds for cards

Animations:
* 1-second ease-linear transitions for progress fill
* Pulse animation (1s infinite) during overtime
* Smooth slider interactions

Responsive Behavior
* Container: Max-width 448px (md breakpoint)
* Height: Fixed 500px for main card
* Padding: Consistent 12-unit padding
* Mobile: Full-width with 8-unit padding

Edge Cases & Error Handling
* Audio Context Initialization: Wrapped in try-catch to handle browser restrictions
* localStorage Unavailable: Graceful degradation without persistence
* Timer Precision: Using setInterval with 1000ms updates
* Overtime Limit: No maximum overtime duration
* History Overflow: Scrollable container for unlimited rounds
* Audio Playback Failure: Silent fallback with console logging

## Technical Stack

* Language: Kotlin
* Framework: Kotlin Multiplatform
* UI: Compose Multiplatform
* State Management: Kotlin Coroutines & StateFlow
* Targets: Android, iOS, Desktop (JVM), Web (Wasm)


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

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
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