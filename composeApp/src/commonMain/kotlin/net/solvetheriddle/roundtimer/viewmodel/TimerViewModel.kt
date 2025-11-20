@file:OptIn(ExperimentalTime::class)

package net.solvetheriddle.roundtimer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import net.solvetheriddle.roundtimer.audio.AudioScheduler
import net.solvetheriddle.roundtimer.model.AudioCue
import net.solvetheriddle.roundtimer.model.AudioPattern
import net.solvetheriddle.roundtimer.model.Game
import net.solvetheriddle.roundtimer.model.ScheduledSound
import net.solvetheriddle.roundtimer.model.Sound
import net.solvetheriddle.roundtimer.model.Round
import net.solvetheriddle.roundtimer.model.TimerState
import net.solvetheriddle.roundtimer.platform.getScreenLocker
import net.solvetheriddle.roundtimer.platform.getSoundPlayer
import net.solvetheriddle.roundtimer.platform.getAnalyticsService
import net.solvetheriddle.roundtimer.storage.RoundTimerStorage
import net.solvetheriddle.roundtimer.storage.createPlatformStorage
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

private const val UPDATE_INTERVAL = 50L

class TimerViewModel : ViewModel() {

    private val storage by lazy {
        RoundTimerStorage(createPlatformStorage())
    }
    private val soundPlayer by lazy { getSoundPlayer() }
    private val screenLocker by lazy { getScreenLocker() }
    private val analyticsService by lazy { getAnalyticsService() }
    private val audioScheduler by lazy { AudioScheduler(soundPlayer, viewModelScope) }
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    init {
        // Initialize storage and load saved data
        try {
            storage.initialize()
            viewModelScope.launch {
                try {
                    val savedRounds = storage.loadRounds()
                    val configuredTime = storage.loadConfiguredTime() ?: _state.value.configuredTime
                    val games = storage.loadGames().sortedByDescending { it.id }
                    val activeGameId = storage.loadActiveGameId() ?: games.firstOrNull()?.id
                    val savedSettings = storage.loadSettings() ?: _state.value.settings
                    _state.value = _state.value.copy(
                        rounds = savedRounds,
                        configuredTime = configuredTime,
                        currentTime = configuredTime,
                        games = games,
                        activeGameId = activeGameId,
                        settings = savedSettings,
                        // Load types from the active game
                        customTypes = games.find { it.id == activeGameId }?.customTypes ?: emptyList(),
                        playerTypes = games.find { it.id == activeGameId }?.playerTypes ?: emptyList()
                    )

                } catch (e: Exception) {
                    // Continue with empty list
                }
            }
        } catch (e: Exception) {
            // Continue with default state
        }
    }

    private var timerJob: Job? = null
    private var timerStartTime: TimeSource.Monotonic.ValueTimeMark? = null
    private var initialTimerDuration: Long = 0L
    private var fastForwardOffset: Long = 0L

    private var deletedRound: Round? = null
    private var deletedGame: Game? = null

    fun updateConfiguredTime(seconds: Int) {
        if (!_state.value.isRunning) {
            val milliseconds = seconds * 1000L
            _state.value = _state.value.copy(
                configuredTime = milliseconds,
                currentTime = milliseconds
            )
            analyticsService.logEvent("configured_time_updated", mapOf("seconds" to seconds.toString()))
            viewModelScope.launch {
                storage.saveConfiguredTime(milliseconds)
            }
        }
    }

    private fun getCurrentDate(): String {
        val now = Clock.System.now()
        val zone = TimeZone.currentSystemDefault()
        val localDate = now.toLocalDateTime(zone)
        val dateFormat = LocalDateTime.Format {
            dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
            char(' ')
            day(Padding.NONE)
            char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            yearTwoDigits(2000)
        }
        return dateFormat.format(localDate)
    }

    fun startTimer() {
        screenLocker.lock()
        var currentState = _state.value
        if (currentState.activeGameId == null) {
            val newGame = Game(id = Clock.System.now().toEpochMilliseconds().toString(), date = getCurrentDate(), name = "")
            val newGames = currentState.games + newGame
            currentState = currentState.copy(games = newGames, activeGameId = newGame.id)
            analyticsService.logEvent(
                "game_created", mapOf("game_id" to newGame.id, "game_date" to newGame.date, "game_name" to newGame.name)
            )
            viewModelScope.launch {
                storage.saveGames(newGames)
            }
        }

        _state.value = currentState.copy(
            isRunning = true,
            currentTime = currentState.configuredTime,
            overtimeTime = 0L,
            isOvertime = false,
            startTimestamp = Clock.System.now().toEpochMilliseconds()
        )
        analyticsService.logEvent(
            "timer_started", mapOf(
                "game_id" to currentState.activeGameId!!,
                "configured_time" to currentState.configuredTime.toString(),
                "subtle_drumming" to currentState.settings.isSubtleDrummingEnabled.toString(),
                "intense_drumming" to currentState.settings.isIntenseDrummingEnabled.toString(),
                "overtime_alarm" to currentState.settings.isOvertimeAlarmEnabled.toString(),
                "timeout_gong" to currentState.settings.isTimeoutGongEnabled.toString(),
                "jonas_scolding" to currentState.settings.isJonasScoldingEnabled.toString()
            )
        )

        startCountdownWithPreciseAudio()
    }

    @OptIn(ExperimentalTime::class)
    private fun startCountdownWithPreciseAudio() {
        val configuredTime = _state.value.configuredTime
        initialTimerDuration = configuredTime
        timerStartTime = TimeSource.Monotonic.markNow()
        fastForwardOffset = 0L

        // Pre-calculate all scheduled audio events
        val audioEvents = createAudioSchedule(configuredTime)

        // Start the precise audio scheduler
        audioScheduler.start(audioEvents)

        // Start the UI update timer using the same time source
        startSynchronizedCountdown()
    }

    private fun createAudioSchedule(timerDurationMs: Long): List<ScheduledSound> {
        val events = mutableListOf<ScheduledSound>()
        val audioCues = generateAudioCues()
        audioCues.forEach { cue ->
            val triggerTimeMs = timerDurationMs - (cue.threshold * 1000L)
            if (triggerTimeMs >= 0) {
                events.add(ScheduledSound(triggerTimeMs, cue.sound, cue.pattern))
            }
        }

        // Add overtime sound schedule
        val overtimeCues = createOvertimeSchedule(timerDurationMs)
        events.addAll(overtimeCues)

        return events.sortedBy { it.triggerTimeMs }
    }

    private fun generateAudioCues(): List<AudioCue> {
        val settings = _state.value.settings
        val audioCues = mutableListOf<AudioCue>()
        if (settings.isSubtleDrummingEnabled) {
            val subtleDrumRepeatCount = if (settings.isIntenseDrummingEnabled) 4 else 6
            audioCues.add(
                AudioCue(threshold = 60, sound = Sound.CALL, pattern = AudioPattern.Repeated(subtleDrumRepeatCount, 10 * 1000L))
            )
        }
        if (settings.isIntenseDrummingEnabled) {
            audioCues.add(
                AudioCue(threshold = 21, sound = Sound.INTENSE)
            )
        }
        if (settings.isTimeoutGongEnabled) {
            audioCues.add(
                AudioCue(threshold = 0, sound = Sound.TIMEOUT_GONG)
            )
        }
        return audioCues
    }

    /**
     * Creates a schedule of overtime sounds that play after the timer expires.
     * You can customize this pattern based on your needs.
     */
    private fun createOvertimeSchedule(timerDurationMs: Long): List<ScheduledSound> {
        val overtimeEvents = mutableListOf<ScheduledSound>()

        val settings = _state.value.settings
        if (settings.isOvertimeAlarmEnabled) {
            repeat(8) { seconds ->
                val triggerTime = timerDurationMs + (seconds * 1000L)
                overtimeEvents.add(ScheduledSound(triggerTime, Sound.OVERTIME))
            }
            for (seconds in 14 until 150 step 3) {
                val triggerTime = timerDurationMs + (seconds * 800L)
                overtimeEvents.add(ScheduledSound(triggerTime, Sound.OVERTIME))
            }
        }
        if (settings.isJonasScoldingEnabled) {
            val overtimeCalls = listOf(
                Sound.OVERTIME_CALL1,
                Sound.OVERTIME_CALL1,
                Sound.OVERTIME_CALL2,
                Sound.OVERTIME_CALL2,
                Sound.OVERTIME_CALL3,
                Sound.OVERTIME_CALL3,
                Sound.OVERTIME_CALL4,
                Sound.OVERTIME_CALL5,
                Sound.OVERTIME_CALL6,
            )
            overtimeEvents.add(ScheduledSound(timerDurationMs + (8 * 1000L), overtimeCalls.random()))
            overtimeEvents.add(ScheduledSound(timerDurationMs + (14 * 1000L), overtimeCalls.random()))
        }

        return overtimeEvents
    }

    @OptIn(ExperimentalTime::class)
    private fun startSynchronizedCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.isRunning) {
                delay(UPDATE_INTERVAL)

                val currentState = _state.value
                val elapsedTime = (timerStartTime?.elapsedNow()?.inWholeMilliseconds ?: 0L) + fastForwardOffset

                if (elapsedTime < initialTimerDuration) {
                    // Normal countdown - calculate remaining time based on elapsed time
                    val remainingTime = maxOf(0L, initialTimerDuration - elapsedTime)
                    _state.value = currentState.copy(currentTime = remainingTime)
                } else {
                    // Overtime mode - calculate overtime based on elapsed time
                    val overtimeTime = elapsedTime - initialTimerDuration
                    _state.value = currentState.copy(
                        currentTime = 0L,
                        isOvertime = true,
                        overtimeTime = overtimeTime
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun stopTimer() {
        screenLocker.unlock()
        timerJob?.cancel()
        audioScheduler.stop()

        val currentState = _state.value
        if (currentState.isRunning) {
            // Save round to history
            val newRound = Round(
                id = uuid(),
                duration = _state.value.currentTime.toInt(),
                overtime = _state.value.overtimeTime.toInt(),
                timestamp = Clock.System.now().toEpochMilliseconds(),
                gameId = _state.value.activeGameId ?: "",
                category = _state.value.selectedType
            )

            val newRounds = currentState.rounds + newRound
            _state.value = currentState.copy(
                isRunning = false,
                isOvertime = false,
                currentTime = currentState.configuredTime,
                overtimeTime = 0L,
                rounds = newRounds
            )

            analyticsService.logEvent(
                "timer_stopped", mapOf(
                    "game_id" to (currentState.activeGameId ?: ""),
                    "duration" to newRound.duration.toString(),
                    "overtime" to newRound.overtime.toString()
                )
            )
            // Save to storage asynchronously
            viewModelScope.launch {
                try {
                    storage.saveRounds(newRounds)
                } catch (e: Exception) {
                    // Storage save failed, but continue with UI update
                }
            }
        }
    }

    fun deleteRound(roundId: String) {
        val currentState = _state.value
        deletedRound = currentState.rounds.find { it.id == roundId }
        val newRounds = currentState.rounds.filter { it.id != roundId }
        _state.value = currentState.copy(rounds = newRounds)
        analyticsService.logEvent("round_deleted", mapOf("round_id" to roundId, "game_id" to currentState.activeGameId!!))
        viewModelScope.launch {
            try {
                storage.saveRounds(newRounds)
            } catch (e: Exception) {
                // Storage save failed, but continue with UI update
            }
        }
    }

    fun undoDeleteRound() {
        deletedRound?.let {
            val newRounds = (_state.value.rounds + it).sortedBy { it.timestamp }
            _state.value = _state.value.copy(rounds = newRounds)
            analyticsService.logEvent("round_delete_undone", mapOf("game_id" to it.gameId))
            viewModelScope.launch {
                storage.saveRounds(newRounds)
            }
        }
    }

    fun resetHistoryForGame(gameId: String) {
        val currentState = _state.value
        val roundsToDelete = currentState.rounds.filter { it.gameId == gameId }
        val newRounds = currentState.rounds.filter { it.gameId != gameId }
        _state.value = currentState.copy(rounds = newRounds)
        analyticsService.logEvent("history_reset_for_game", mapOf("game_id" to gameId, "rounds_deleted" to roundsToDelete.size.toString()))

        // Clear storage asynchronously
        viewModelScope.launch {
            try {
                storage.saveRounds(newRounds)
            } catch (e: Exception) {
                // Storage clear failed, but continue with UI update
            }
        }
    }

    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) {
            "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
        } else {
            "0:${remainingSeconds.toString().padStart(2, '0')}"
        }
    }

    /**
     * Fast forward the timer by the specified number of seconds.
     * This will immediately advance the timer state and trigger any audio cues
     * that should have played during the skipped time.
     */
    fun fastForward(seconds: Int) {
        if (!_state.value.isRunning || seconds <= 0 || !_state.value.settings.isSecretFastForwardEnabled) {
            return
        }

        val fastForwardMs = seconds * 1000L
        val currentState = _state.value

        // Update the fast forward offset for UI synchronization
        fastForwardOffset += fastForwardMs

        // Fast forward the audio scheduler
        audioScheduler.fastForward(fastForwardMs)

        // The UI will automatically update on the next interval using the synchronized timing
        // with the updated fastForwardOffset

        analyticsService.logEvent(
            "timer_fast_forwarded",
            mapOf(
                "game_id" to (currentState.activeGameId ?: ""),
                "seconds_forwarded" to seconds.toString(),
                "was_overtime" to currentState.isOvertime.toString()
            )
        )
    }

    fun createNewGame(name: String) {
        val existingGame = _state.value.games.find { it.name.equals(name, ignoreCase = true) }
        
        val newGame = Game(
            id = uuid(),
            date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
            name = name,
            // Copy types from existing game if found, otherwise use defaults (empty)
            customTypes = existingGame?.customTypes ?: emptyList(),
            playerTypes = existingGame?.playerTypes ?: emptyList()
        )
        
        val updatedGames = listOf(newGame) + _state.value.games
        _state.value = _state.value.copy(
            games = updatedGames,
            activeGameId = newGame.id,
            customTypes = newGame.customTypes,
            playerTypes = newGame.playerTypes,
            selectedType = "Preparation"
        )
        
        viewModelScope.launch {
            storage.saveGames(updatedGames)
            storage.saveActiveGameId(newGame.id)
        }
        analyticsService.logEvent("create_new_game", mapOf("name" to name))
    }

    fun setActiveGame(gameId: String) {
        val game = _state.value.games.find { it.id == gameId }
        _state.value = _state.value.copy(
            activeGameId = gameId,
            customTypes = game?.customTypes ?: emptyList(),
            playerTypes = game?.playerTypes ?: emptyList(),
            selectedType = "Preparation" // Reset selection when switching games
        )
        analyticsService.logEvent("active_game_changed", mapOf("game_id" to gameId))
        viewModelScope.launch {
            storage.saveActiveGameId(gameId)
        }
    }

    fun updateGameName(gameId: String, name: String) {
        val updatedGames = _state.value.games.map {
            if (it.id == gameId) {
                it.copy(name = name)
            } else {
                it
            }
        }.sortedByDescending { it.id }
        _state.value = _state.value.copy(games = updatedGames)
        analyticsService.logEvent("game_name_updated", mapOf("game_id" to gameId, "new_name" to name))
        viewModelScope.launch {
            storage.saveGames(updatedGames)
        }
    }

    fun updateSetting(settingName: String, isEnabled: Boolean) {
        val currentSettings = _state.value.settings
        val newSettings = when (settingName) {
            "subtleDrumming" -> currentSettings.copy(isSubtleDrummingEnabled = isEnabled)
            "intenseDrumming" -> currentSettings.copy(isIntenseDrummingEnabled = isEnabled)
            "overtimeAlarm" -> {
                if (isEnabled) {
                    currentSettings.copy(isOvertimeAlarmEnabled = true, isTimeoutGongEnabled = false)
                } else {
                    currentSettings.copy(isOvertimeAlarmEnabled = false)
                }
            }
            "timeoutGong" -> {
                if (isEnabled) {
                    currentSettings.copy(isTimeoutGongEnabled = true, isOvertimeAlarmEnabled = false)
                } else {
                    currentSettings.copy(isTimeoutGongEnabled = false)
                }
            }
            "jonasScolding" -> currentSettings.copy(isJonasScoldingEnabled = isEnabled)
            "secretFastForward" -> currentSettings.copy(isSecretFastForwardEnabled = isEnabled)
            else -> currentSettings
        }
        if (newSettings != currentSettings) {
            _state.value = _state.value.copy(settings = newSettings)
            analyticsService.logEvent(
                "setting_updated",
                mapOf("setting_name" to settingName, "is_enabled" to isEnabled.toString())
            )
            viewModelScope.launch {
                storage.saveSettings(newSettings)
            }
        }
    }

    fun deleteGame(gameId: String) {
        val currentState = _state.value
        deletedGame = currentState.games.find { it.id == gameId }
        val newGames = currentState.games.filter { it.id != gameId }.sortedByDescending { it.id }
        val newActiveGameId = if (currentState.activeGameId == gameId) {
            newGames.firstOrNull()?.id
        } else {
            currentState.activeGameId
        }
        _state.value = currentState.copy(games = newGames, activeGameId = newActiveGameId)
        analyticsService.logEvent("game_deleted", mapOf("game_id" to gameId))
        viewModelScope.launch {
            storage.saveGames(newGames)
            storage.saveActiveGameId(newActiveGameId)
        }
    }

    fun undoDeleteGame() {
        deletedGame?.let {
            val newGames = (_state.value.games + it).sortedByDescending { it.id }
            _state.value = _state.value.copy(games = newGames)
            analyticsService.logEvent("game_delete_undone", mapOf("game_id" to it.id))
            viewModelScope.launch {
                storage.saveGames(newGames)
            }
        }
    }

    // Category Management
    fun selectCategory(category: String) {
        _state.value = _state.value.copy(selectedType = category)
        analyticsService.logEvent("select_category", mapOf("category" to category))
    }

    fun addCustomCategory(category: String) {
        val newTypes = _state.value.customTypes + category
        _state.value = _state.value.copy(customTypes = newTypes)
        updateActiveGameTypes(newTypes, _state.value.playerTypes)
        analyticsService.logEvent("add_custom_category", mapOf("category" to category))
    }

    fun removeCustomCategory(category: String) {
        val newTypes = _state.value.customTypes - category
        _state.value = _state.value.copy(
            customTypes = newTypes,
            selectedType = if (_state.value.selectedType == category) "Preparation" else _state.value.selectedType
        )
        updateActiveGameTypes(newTypes, _state.value.playerTypes)
        analyticsService.logEvent("remove_custom_category", mapOf("category" to category))
    }

    fun addPlayerCategory(player: String) {
        val newTypes = _state.value.playerTypes + player
        _state.value = _state.value.copy(playerTypes = newTypes)
        updateActiveGameTypes(_state.value.customTypes, newTypes)
        analyticsService.logEvent("add_player_category", mapOf("category" to player))
    }

    fun removePlayerCategory(player: String) {
        val newTypes = _state.value.playerTypes - player
        _state.value = _state.value.copy(
            playerTypes = newTypes,
            selectedType = if (_state.value.selectedType == player) "Everyone" else _state.value.selectedType
        )
        updateActiveGameTypes(_state.value.customTypes, newTypes)
        analyticsService.logEvent("remove_player_category", mapOf("category" to player))
    }

    fun renameCustomCategory(oldName: String, newName: String) {
        val newTypes = _state.value.customTypes.map { if (it == oldName) newName else it }
        _state.value = _state.value.copy(
            customTypes = newTypes,
            selectedType = if (_state.value.selectedType == oldName) newName else _state.value.selectedType
        )
        updateActiveGameTypes(newTypes, _state.value.playerTypes)
        analyticsService.logEvent("rename_custom_category", mapOf("oldName" to oldName, "newName" to newName))
    }

    fun renamePlayerCategory(oldName: String, newName: String) {
        val newTypes = _state.value.playerTypes.map { if (it == oldName) newName else it }
        _state.value = _state.value.copy(
            playerTypes = newTypes,
            selectedType = if (_state.value.selectedType == oldName) newName else _state.value.selectedType
        )
        updateActiveGameTypes(_state.value.customTypes, newTypes)
        analyticsService.logEvent("rename_player_category", mapOf("oldName" to oldName, "newName" to newName))
    }

    private fun updateActiveGameTypes(customTypes: List<String>, playerTypes: List<String>) {
        val activeGameId = _state.value.activeGameId ?: return
        val updatedGames = _state.value.games.map { game ->
            if (game.id == activeGameId) {
                game.copy(customTypes = customTypes, playerTypes = playerTypes)
            } else {
                game
            }
        }
        _state.value = _state.value.copy(games = updatedGames)
        viewModelScope.launch {
            storage.saveGames(updatedGames)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        audioScheduler.stop()
        soundPlayer.cleanup()
    }

    private fun uuid(): String {
        val charPool = "0123456789abcdef"
        return (1..32)
            .map { charPool[Random.nextInt(0, charPool.length)] }
            .joinToString("")
            .let { 
                "${it.substring(0, 8)}-${it.substring(8, 12)}-${it.substring(12, 16)}-${it.substring(16, 20)}-${it.substring(20, 32)}"
            }
    }
}