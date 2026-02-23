# RoundTimer — Full Codebase Review

> **Date**: 2026-02-23  
> **Reviewer**: AI code-review agent  
> **Scope**: All Kotlin source files across all platforms (Android, iOS, JVM, WASM, JS)

---

## Table of Contents

1. [Summary](#1-summary)
2. [Severity & Effort Legend](#2-severity--effort-legend)
3. [Issues](#3-issues)
   - [Critical](#critical)
   - [High](#high)
   - [Medium](#medium)
   - [Low](#low)
4. [RICE-Scored Priority Order](#4-rice-scored-priority-order)

---

## 1. Summary

| Severity  | Count |
|-----------|-------|
| Critical  | 4     |
| High      | 4     |
| Medium    | 6     |
| Low       | 5     |
| **Total** | **19**|

The codebase has a solid multiplatform architecture and clean separation of concerns.
The most urgent problems are a multi-platform compilation failure (missing `actual` implementations),
two crash-risk null-assertions, a data race in the audio scheduler, and the near-total absence of
automated tests.

---

## 2. Severity & Effort Legend

### Severity

| Level    | Meaning                                                    |
|----------|------------------------------------------------------------|
| Critical | Prevents compilation or causes guaranteed runtime crashes  |
| High     | Likely to cause data loss, corruption, or UX-breaking bugs |
| Medium   | Intermittent bugs, silent failures, resource leaks         |
| Low      | Code quality, dead code, style, minor maintainability debt |

### Effort

| Label | Typical scope                                           |
|-------|---------------------------------------------------------|
| XS    | A few lines in a single file, ≤ 1 hour                 |
| S     | One file, a handful of changes, ≤ half a day           |
| M     | Multiple files, significant logic change, 1–2 days     |
| L     | Cross-cutting change touching several modules, 3–5 days|
| XL    | Architectural or test-suite–scale work, > 1 week       |

---

## 3. Issues

### Critical

---

#### C-1 · Missing `actual fun cleanup()` in iOS, JS, and WASM SoundPlayer

| Field    | Value |
|----------|-------|
| **Severity** | Critical |
| **Effort**   | XS |
| **Files**    | `iosMain/platform/SoundPlayer.ios.kt`, `jsMain/platform/SoundPlayer.js.kt`, `wasmJsMain/platform/SoundPlayer.wasmJs.kt` |

**Problem**  
The `expect class SoundPlayer` in `commonMain` declares three functions:
`playSound()`, `stopSound()`, and `cleanup()`.  
The iOS, JS, and WASM `actual class SoundPlayer` implementations provide only `playSound()` and
`stopSound()` — `cleanup()` is absent.  
In Kotlin Multiplatform every `expect` declaration must be matched by a corresponding `actual`
declaration in every target. The omission causes a **compilation error** for iOS, JS, and WASM
targets.

**Evidence**
```kotlin
// commonMain/platform/SoundPlayer.kt
expect class SoundPlayer {
    fun playSound(sound: Sound)
    fun stopSound()
    fun cleanup()          // ← declared here …
}

// iosMain/platform/SoundPlayer.ios.kt (and js / wasmJs)
actual class SoundPlayer {
    actual fun playSound(sound: Sound) { … }
    actual fun stopSound() { … }
    // … but never implemented here
}
```

**Fix**  
Add `actual fun cleanup()` to each of the three files.  
For iOS: stop and nullify `player`.  
For JS/WASM: pause and nullify `audio`.

---

#### C-2 · Non-null assertion (`!!`) on nullable `activeGameId`

| Field    | Value |
|----------|-------|
| **Severity** | Critical |
| **Effort**   | XS |
| **Files**    | `commonMain/viewmodel/TimerViewModel.kt` lines 172 and 360 |

**Problem**  
Two analytics calls use the Kotlin not-null assertion operator (`!!`) on `activeGameId`, which is
declared `String?`:

```kotlin
// line 172 – inside startTimer()
"game_id" to currentState.activeGameId!!,

// line 360 – inside deleteRound()
analyticsService.logEvent("round_deleted", mapOf("round_id" to roundId,
    "game_id" to currentState.activeGameId!!))
```

Both throw `NullPointerException` in the edge case where no game is active.  
`startTimer()` creates a game just before this line, so in practice the risk there is low — but the
contract is not type-safe and future refactoring could break it silently.  
`deleteRound()` is called from the UI at any time and the null case is genuinely reachable.

**Fix**  
Replace `!!` with safe access or a fallback:
```kotlin
"game_id" to (currentState.activeGameId ?: ""),
```

---

#### C-3 · `runBlocking` inside `SoundPlayer.playSound()` on iOS blocks the main thread

| Field    | Value |
|----------|-------|
| **Severity** | Critical |
| **Effort**   | M |
| **Files**    | `iosMain/platform/SoundPlayer.ios.kt` line 24 |

**Problem**  
Every call to `playSound()` on iOS reads the audio file synchronously from the bundle using
`runBlocking`:

```kotlin
actual fun playSound(sound: Sound) {
    val soundData = runBlocking { resource.readBytes() }   // ← blocks the calling thread
    …
}
```

`playSound()` is called from `AudioScheduler`, which runs inside `viewModelScope` coroutines
dispatched on the main dispatcher (Compose / iOS main thread). Blocking the main thread causes
visible UI jank and, if the I/O takes long enough, an ANR-equivalent freeze.  
This is especially harmful because it happens every time a sound plays — potentially several times
per second during overtime.

**Fix**  
Pre-load sound data once during initialization (as the Android and JVM implementations do), then
play from the in-memory cache synchronously.

---

#### C-4 · Race condition in `AudioScheduler.fastForward()`

| Field    | Value |
|----------|-------|
| **Severity** | Critical |
| **Effort**   | M |
| **Files**    | `commonMain/audio/AudioScheduler.kt` lines 55–76 |

**Problem**  
`fastForward()` mutates `timeOffset` and `scheduledSounds` from different coroutines without
consistent synchronization:

```kotlin
fun fastForward(fastForwardMs: Long) {
    timeOffset += fastForwardMs              // 1. Updated immediately on caller's thread

    val currentTime = getCurrentElapsedTime()

    scope.launch {                           // 2. Mutation of scheduledSounds is deferred
        mutex.withLock {
            scheduledSounds.removeAll { it.triggerTimeMs <= currentTime }
            scheduledSounds.forEach { it.triggerTimeMs -= currentTime }
        }
    }

    schedulerJob?.cancel()                   // 3. New scheduler starts before coroutine in step 2
    startTime = TimeSource.Monotonic.markNow()
    timeOffset = 0                           // 4. timeOffset reset wipes the step-1 update!
    schedulerJob = scope.launch { processScheduledSounds() }
}
```

Four problems:
- Step 1 sets `timeOffset` to the offset, but step 4 resets it to `0` — the offset is effectively lost.
- The mutation coroutine launched in step 2 may execute *after* the new `processScheduledSounds()` coroutine has already started reading `scheduledSounds` (step 3/4), producing duplicate or skipped sounds.
- `scheduledSounds.forEach { it.triggerTimeMs -= currentTime }` mutates `ScheduledSound.triggerTimeMs` (a `var` field), making the list inconsistent if read concurrently.
- `schedulerJob` and `startTime` are mutated without the mutex, so `processScheduledSounds()` can see a partially-updated state.

**Fix**  
Perform the entire fast-forward operation inside `mutex.withLock`, including cancelling the old job
and restarting the scheduler, to make it atomic from the perspective of `processScheduledSounds`.

---

### High

---

#### H-1 · `RoundTimerStorage.clearAll()` only clears rounds

| Field    | Value |
|----------|-------|
| **Severity** | High |
| **Effort**   | XS |
| **Files**    | `commonMain/storage/StorageManager.kt` lines 189–196 |

**Problem**  
`clearAll()` only removes `ROUNDS_KEY`:

```kotlin
suspend fun clearAll() {
    cachedRounds = emptyList()
    platformStorage.remove(ROUNDS_KEY)     // ← only rounds removed
    // games, active game ID, settings, and configured time are NOT cleared
}
```

Callers expecting a full reset will be surprised to find games, settings, and configured time
persisted after the call.

**Fix**  
Remove all five storage keys: `ROUNDS_KEY`, `GAMES_KEY`, `CONFIGURED_TIME_KEY`,
`ACTIVE_GAME_ID_KEY`, and `SETTINGS_KEY`, or delegate to `platformStorage.clear()`.

---

#### H-2 · `GlobalScope` usage in JVM `SoundPlayer` causes unmanaged coroutine leaks

| Field    | Value |
|----------|-------|
| **Severity** | High |
| **Effort**   | S |
| **Files**    | `jvmMain/platform/SoundPlayer.jvm.kt` lines 40, 81, 97 |

**Problem**  
Three `GlobalScope.launch` calls create coroutines that are not tied to any lifecycle:

```kotlin
// line 40 – initialization
GlobalScope.launch(Dispatchers.IO) { initializeAudioSystem() }

// line 81 – clip release callback (called for every clip stop event)
GlobalScope.launch { releaseClip(sound, AudioClip(clip, false)) }

// line 97 – every playSound() call
GlobalScope.launch { playAudioAsync(sound) }
```

These coroutines cannot be cancelled when `cleanup()` is called, so they can continue running and
accessing already-freed resources (clips, pools). This is especially problematic when the ViewModel
is destroyed and `cleanup()` is supposed to tear everything down.

**Fix**  
Replace all `GlobalScope.launch` calls with a locally owned `CoroutineScope` (similar to the
Android implementation's `CoroutineScope(SupervisorJob() + Dispatchers.Main)`). Cancel the scope in
`cleanup()`.

---

#### H-3 · `pendingSound` in Android `SoundPlayer` drops all but the last queued sound

| Field    | Value |
|----------|-------|
| **Severity** | High |
| **Effort**   | S |
| **Files**    | `androidMain/platform/SoundPlayer.android.kt` lines 44, 74–78, 116–119 |

**Problem**  
If sounds are requested before the `SoundPool` finishes loading, only the *last* sound is queued:

```kotlin
private var pendingSound: Sound? = null   // ← single slot

actual fun playSound(sound: Sound) {
    if (!soundsLoaded) {
        pendingSound = sound   // ← overwrites any previously queued sound
        return
    }
    …
}
```

At app startup, the timer configuration screen typically triggers several audio cues in quick
succession. All but the last will be silently dropped.

**Fix**  
Change `pendingSound` to a `Queue<Sound>` and drain the queue once loading completes.

---

#### H-4 · No meaningful automated tests

| Field    | Value |
|----------|-------|
| **Severity** | High |
| **Effort**   | XL |
| **Files**    | `commonTest/kotlin/net/solvetheriddle/roundtimer/ComposeAppCommonTest.kt` |

**Problem**  
The entire test suite consists of a single placeholder assertion:

```kotlin
@Test
fun example() {
    assertEquals(3, 1 + 2)
}
```

There are no tests for:
- Timer countdown accuracy / overtime calculation
- Audio schedule generation (`createAudioSchedule`, `createOvertimeSchedule`)
- `AudioScheduler` timing logic and fast-forward behaviour
- `RoundTimerStorage` serialization / deserialization round-trips
- `TimerViewModel` state transitions (start, stop, reset, fast-forward)
- Game / round CRUD operations
- Settings persistence

This means regressions in any core business logic go undetected until manual testing.

**Fix**  
Start with unit tests for `TimerViewModel` state transitions and `RoundTimerStorage`
serialization, as these are pure Kotlin with no platform dependencies and yield the highest
confidence gain per test written.

---

### Medium

---

#### M-1 · `cachedRounds` in `RoundTimerStorage` is not thread-safe

| Field    | Value |
|----------|-------|
| **Severity** | Medium |
| **Effort**   | S |
| **Files**    | `commonMain/storage/StorageManager.kt` lines 25, 66, 84–91 |

**Problem**  
`cachedRounds` is a plain `var List<Round>` written from at least two different coroutine scopes:
`storageScope` (inside `initialize()`) and `viewModelScope` (inside `saveRounds()` and
`loadRounds()`). Kotlin's `MutableStateFlow` guarantees are not involved here — this is a raw
field on a non-thread-safe class. Concurrent writes can produce torn reads or lost updates.

**Fix**  
Protect access with a `Mutex` or change the field to an `AtomicReference` / `@Volatile`.
Alternatively, remove the cache entirely since the ViewModel already keeps authoritative state in
its `StateFlow`.

---

#### M-2 · `storageScope` in `RoundTimerStorage` is never cancelled

| Field    | Value |
|----------|-------|
| **Severity** | Medium |
| **Effort**   | S |
| **Files**    | `commonMain/storage/StorageManager.kt` line 27 |

**Problem**  
```kotlin
private val storageScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
```

This scope is used to load initial data in `initialize()` but is never cancelled.  
If the `ViewModel` (and therefore `RoundTimerStorage`) is recreated (e.g., during tests or
configuration changes on some platforms), the old `storageScope` continues to run indefinitely.

**Fix**  
Either cancel `storageScope` from `RoundTimerStorage.close()` / ViewModel's `onCleared()`, or
remove the internal scope and accept a `CoroutineScope` parameter (dependency injection).

---

#### M-3 · JVM `SoundPlayer` shared `isInitialized` flag causes reinitialisation failure

| Field    | Value |
|----------|-------|
| **Severity** | Medium |
| **Effort**   | S |
| **Files**    | `jvmMain/platform/SoundPlayer.jvm.kt` lines 34–35, 46–47 |

**Problem**  
`isInitialized` lives in the `companion object`, making it a class-level singleton flag:

```kotlin
companion object {
    private var isInitialized = false
    private val initMutex = Mutex()
}
```

`getSoundPlayer()` returns `SoundPlayer()` — a **new instance** each time — but
`initializeAudioSystem()` returns early if `isInitialized` is true. So if the first `SoundPlayer`
instance is cleaned up (`cleanup()` clears `clipPools` and `audioCache`), and a new instance is
created, it finds `isInitialized = true` and skips initialization, leaving the new instance with
empty pools. Every subsequent `playSound()` call silently does nothing.

**Fix**  
Move `isInitialized` and `initMutex` to instance scope, or make `getSoundPlayer()` return a
singleton on JVM (matching Android's approach).

---

#### M-4 · `activeStreams` list in Android `SoundPlayer` is not thread-safe

| Field    | Value |
|----------|-------|
| **Severity** | Medium |
| **Effort**   | S |
| **Files**    | `androidMain/platform/SoundPlayer.android.kt` lines 41, 133, 142–143 |

**Problem**  
`activeStreams` is a plain `mutableListOf<Int>()`:

```kotlin
private val activeStreams = mutableListOf<Int>()

// written on the main thread (scope.launch → Dispatchers.Main)
activeStreams.add(it)

// cleared on the main thread inside stopSound()
activeStreams.forEach { soundPool?.stop(it) }
activeStreams.clear()
```

Both accesses are on `Dispatchers.Main`, so the risk is low today.  
However, `cleanup()` is called from `onCleared()` which may run on any thread, and any future
concurrent access will corrupt the list.

**Fix**  
Replace with `CopyOnWriteArrayList` or protect access with the same `scope` already used for
playback.

---

#### M-5 · Silent exception swallowing hides storage failures

| Field    | Value |
|----------|-------|
| **Severity** | Medium |
| **Effort**   | S |
| **Files**    | `commonMain/storage/StorageManager.kt`, `commonMain/viewmodel/TimerViewModel.kt` |

**Problem**  
Multiple `catch (e: Exception) { }` or `catch (e: Exception) { // … }` blocks silently discard
exceptions without logging:

```kotlin
// StorageManager.kt – loadGames()
} catch (e: Exception) {
    // Return empty list if loading fails
}

// TimerViewModel.kt – stopTimer()
} catch (e: Exception) {
    // Storage save failed, but continue with UI update
}
```

When storage operations fail in production, there is no way to diagnose the root cause.

**Fix**  
At minimum, log the exception message at each catch site. Prefer a structured logging utility
(e.g., platform-specific `Log.e` on Android) over `println`.

---

#### M-6 · `RoundTimerStorage.saveRounds()` sets cache before the write succeeds

| Field    | Value |
|----------|-------|
| **Severity** | Medium |
| **Effort**   | XS |
| **Files**    | `commonMain/storage/StorageManager.kt` lines 64–73 |

**Problem**  
```kotlin
suspend fun saveRounds(rounds: List<Round>) {
    try {
        cachedRounds = rounds           // ← cache updated optimistically
        val jsonString = json.encodeToString(rounds)
        platformStorage.saveString(ROUNDS_KEY, jsonString)
    } catch (e: Exception) {
        cachedRounds = rounds           // ← set again redundantly in the catch
        throw StorageException(…, e)
    }
}
```

If `encodeToString` or `saveString` throws, the cache reflects rounds that were never persisted.
On the next app launch, `loadRounds()` will return stale data from storage, not from the cache
(cache is in memory only), potentially losing the most recent round.

**Fix**  
Move `cachedRounds = rounds` to after `saveString` succeeds (i.e., after the try block).

---

### Low

---

#### L-1 · Deprecated `selectedType` field retained in `TimerState`

| Field    | Value |
|----------|-------|
| **Severity** | Low |
| **Effort**   | XS |
| **Files**    | `commonMain/model/TimerState.kt` line 14 |

**Problem**  
```kotlin
val selectedType: String = "Preparation", // Deprecated
```

The field is never read anywhere in the codebase. It adds noise to the model and to every debug
print of the state.

**Fix**  
Remove the field. If backward-compatible deserialization is needed (old saved JSON contains this
key), the `Json { ignoreUnknownKeys = true }` configuration already handles that silently.

---

#### L-2 · Scaffolding files `Greeting.kt` and `Platform.kt` are dead code

| Field    | Value |
|----------|-------|
| **Severity** | Low |
| **Effort**   | XS |
| **Files**    | `commonMain/Greeting.kt`, `commonMain/Platform.kt`, corresponding `Platform.*.kt` files in each target |

**Problem**  
`Greeting` and the associated `Platform` interface / `getPlatform()` expect/actual declarations are
KMP project-template boilerplate that is never used in the application.

**Fix**  
Delete `Greeting.kt`, `Platform.kt`, and all `Platform.*.kt` actual files.

---

#### L-3 · `println()` used for all logging in production code

| Field    | Value |
|----------|-------|
| **Severity** | Low |
| **Effort**   | S |
| **Files**    | `androidMain/platform/SoundPlayer.android.kt`, `jvmMain/platform/SoundPlayer.jvm.kt`, `iosMain/platform/SoundPlayer.ios.kt`, `jsMain/platform/SoundPlayer.js.kt`, `iosMain/platform/AnalyticsService.ios.kt`, and others (~26 occurrences) |

**Problem**  
All diagnostic output uses `println()`, which:
- Is not filterable by tag or severity
- Is not suppressed in release builds on Android
- Provides no stack trace without extra code

**Fix**  
Introduce a minimal `Logger` expect/actual (e.g., wrapping `android.util.Log` on Android and
`println` on other platforms) and replace all `println` calls with structured `Logger.d` / `Logger.e`
calls. On Android, guard verbose logs with `BuildConfig.DEBUG`.

---

#### L-4 · Pointless catch-rethrow in Android `PlatformStorage`

| Field    | Value |
|----------|-------|
| **Severity** | Low |
| **Effort**   | XS |
| **Files**    | `androidMain/storage/PlatformStorage.android.kt` lines 27–29, 54–56 |

**Problem**  
```kotlin
actual suspend fun saveString(key: String, value: String) {
    try {
        …
    } catch (e: Exception) {
        throw e           // ← identical to having no try/catch
    }
}
```

The catch block adds no value (no logging, no wrapping, no recovery).

**Fix**  
Remove the try/catch blocks entirely, or log the exception before rethrowing.

---

#### L-5 · Only one pending sound slot in Android `SoundPlayer`

*(Already covered as H-3; listed here for cross-reference in the Low section since the visible
symptom — missing a cue — can be mistaken for a minor audio glitch.)*

---

## 4. RICE-Scored Priority Order

RICE score = **(Reach × Impact × Confidence) / Effort**

| # | Issue | Severity | Effort | R | I | C | Effort (days) | RICE |
|---|-------|----------|--------|---|---|---|---------------|------|
| 1 | **C-2** Non-null `!!` crash risk | Critical | XS | 5 | 5 | 0.9 | 0.5 | **45.0** |
| 2 | **C-1** Missing `actual fun cleanup()` (iOS/JS/WASM) | Critical | XS | 4 | 5 | 1.0 | 0.5 | **40.0** |
| 3 | **H-1** `clearAll()` incomplete | High | XS | 3 | 4 | 0.9 | 0.5 | **21.6** |
| 4 | **M-6** Cache set before write succeeds | Medium | XS | 4 | 3 | 0.9 | 0.5 | **21.6** |
| 5 | **M-1** `cachedRounds` not thread-safe | Medium | S | 4 | 3 | 0.8 | 1 | **9.6** |
| 6 | **H-2** `GlobalScope` in JVM SoundPlayer | High | S | 2 | 4 | 0.8 | 1 | **6.4** |
| 7 | **M-4** `activeStreams` not thread-safe (Android) | Medium | S | 3 | 3 | 0.7 | 1 | **6.3** |
| 8 | **M-2** `storageScope` never cancelled | Medium | S | 3 | 2 | 0.8 | 1 | **4.8** |
| 9 | **H-3** `pendingSound` drops queued sounds (Android) | High | S | 3 | 2 | 0.8 | 1 | **4.8** |
| 10 | **M-3** JVM `isInitialized` shared state bug | Medium | S | 2 | 3 | 0.8 | 1 | **4.8** |
| 11 | **L-4** Pointless catch-rethrow (Android storage) | Low | XS | 2 | 2 | 1.0 | 0.5 | **8.0** |
| 12 | **C-3** `runBlocking` blocks iOS main thread | Critical | M | 4 | 4 | 0.9 | 3 | **4.8** |
| 13 | **C-4** Race condition in `AudioScheduler.fastForward()` | Critical | M | 3 | 4 | 0.8 | 3 | **3.2** |
| 14 | **M-5** Silent exception swallowing | Medium | S | 5 | 2 | 0.8 | 3 | **2.7** |
| 15 | **H-4** No meaningful automated tests | High | XL | 10 | 5 | 0.9 | 10 | **4.5** |
| 16 | **L-1** Deprecated `selectedType` field | Low | XS | 1 | 1 | 1.0 | 0.5 | **2.0** |
| 17 | **L-2** Dead scaffolding code | Low | XS | 1 | 1 | 1.0 | 0.5 | **2.0** |
| 18 | **L-3** `println()` for all logging | Low | S | 5 | 1 | 0.9 | 2 | **2.3** |
| 19 | **M-6** *(see #4)* | — | — | — | — | — | — | — |

> **Note on H-4 (no tests):** Although the RICE score is moderate (4.5) due to high effort, this
> item has the highest long-term compounding value. It is recommended to start on it in parallel
> with the quick-win XS items once the Critical issues are resolved.

### Recommended fix order

```
Phase 1 – Critical & Zero-Effort Quick Wins (do immediately):
  1. C-2  – Fix null assertions (!! → ?: "")            [XS]
  2. C-1  – Add actual fun cleanup() to iOS/JS/WASM     [XS]
  3. H-1  – Fix clearAll() to clear all keys             [XS]
  4. M-6  – Move cache update after successful write     [XS]
  5. L-4  – Remove pointless catch-rethrow               [XS]
  6. L-1  – Remove deprecated selectedType field        [XS]
  7. L-2  – Delete scaffolding files                     [XS]

Phase 2 – Thread-Safety & Lifecycle (short sprint):
  8. M-1  – Protect cachedRounds with Mutex              [S]
  9. H-2  – Replace GlobalScope in JVM SoundPlayer       [S]
  10. M-4  – Make activeStreams thread-safe               [S]
  11. M-2  – Cancel storageScope on cleanup              [S]
  12. H-3  – Queue multiple pending sounds               [S]
  13. M-3  – Fix JVM isInitialized shared state          [S]
  14. M-5  – Log all caught exceptions                   [S]
  15. L-3  – Introduce structured Logger                 [S]

Phase 3 – Complex Logic Fixes:
  16. C-3  – Pre-load audio on iOS (remove runBlocking)  [M]
  17. C-4  – Fix AudioScheduler.fastForward() race       [M]

Phase 4 – Test Coverage (ongoing):
  18. H-4  – Add unit tests for ViewModel & storage      [XL]
```
