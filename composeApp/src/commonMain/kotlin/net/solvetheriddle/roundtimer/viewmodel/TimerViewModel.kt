@file:OptIn(ExperimentalTime::class)

package net.solvetheriddle.roundtimer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import net.solvetheriddle.roundtimer.model.AudioCue
import net.solvetheriddle.roundtimer.model.Game
import net.solvetheriddle.roundtimer.model.Sound
import net.solvetheriddle.roundtimer.model.Round
import net.solvetheriddle.roundtimer.model.TimerState
import net.solvetheriddle.roundtimer.platform.getScreenLocker
import net.solvetheriddle.roundtimer.platform.getSoundPlayer
import net.solvetheriddle.roundtimer.storage.RoundTimerStorage
import net.solvetheriddle.roundtimer.storage.createPlatformStorage
import kotlin.time.ExperimentalTime

private const val UPDATE_INTERVAL = 50L

class TimerViewModel : ViewModel() {

    private val storage by lazy {
        RoundTimerStorage(createPlatformStorage())
    }
    private val soundPlayer by lazy { getSoundPlayer() }
    private val screenLocker by lazy { getScreenLocker() }
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
                    _state.value = _state.value.copy(
                        rounds = savedRounds,
                        configuredTime = configuredTime,
                        currentTime = configuredTime,
                        games = games,
                        activeGameId = activeGameId
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
    private var audioJob: Job? = null

    // Audio cue configuration as per README
    private val audioCues = listOf(
        AudioCue(threshold = 60, sound = Sound.CALL),
        AudioCue(threshold = 50, sound = Sound.CALL),
        AudioCue(threshold = 40, sound = Sound.CALL),
        AudioCue(threshold = 36, sound = Sound.INTENSE),
        AudioCue(threshold = 19, sound = Sound.INTENSE),
        AudioCue(threshold = 0, sound = Sound.OVERTIME),
    )

    fun updateConfiguredTime(seconds: Int) {
        if (!_state.value.isRunning) {
            val milliseconds = seconds * 1000L
            _state.value = _state.value.copy(
                configuredTime = milliseconds,
                currentTime = milliseconds
            )
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
            viewModelScope.launch {
                storage.saveGames(newGames)
            }
        }

        _state.value = currentState.copy(
            isRunning = true,
            currentTime = currentState.configuredTime,
            overtimeTime = 0L,
            isOvertime = false
        )

        startCountdown()
    }

    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.isRunning) {
                delay(UPDATE_INTERVAL)

                val currentState = _state.value
                if (currentState.currentTime > 0) {
                    // Normal countdown
                    val newTime = maxOf(0L, currentState.currentTime - UPDATE_INTERVAL)
                    _state.value = currentState.copy(currentTime = newTime)

                    // Handle audio cues (check every 100ms but only trigger on second boundaries)
                    val currentSeconds = (newTime / 1000).toInt()
                    val previousSeconds = (currentState.currentTime / 1000).toInt()
                    if (currentSeconds != previousSeconds) {
                        handleAudioCues(currentSeconds)
                    }
                } else {
                    // Overtime mode
                    _state.value = currentState.copy(
                        isOvertime = true,
                        overtimeTime = currentState.overtimeTime + UPDATE_INTERVAL
                    )
                    val currentSeconds = _state.value.overtimeSeconds
                    val previousSeconds = currentState.overtimeSeconds
                    if (currentSeconds != previousSeconds) {
                        playSound(Sound.OVERTIME)
                    }
                }
            }
        }
    }

    private suspend fun handleAudioCues(remainingSeconds: Int) {
        val applicableCue = audioCues.find { it.threshold == remainingSeconds }
        applicableCue?.let { cue ->
            playSound(cue.sound)
            when {
                cue.sound == Sound.DUM && (remainingSeconds == 50 || remainingSeconds == 40) -> {
                    delay(500)
                    playSound(cue.sound)
                }
            }
        }
    }

    private fun playSound(soundName: Sound) {
        soundPlayer.playSound(soundName)
    }

    @OptIn(ExperimentalTime::class)
    fun stopTimer() {
        screenLocker.unlock()
        timerJob?.cancel()
        audioJob?.cancel()
        soundPlayer.stopSound()

        val currentState = _state.value
        if (currentState.isRunning) {
            // Save round to history
            val currentTime = Clock.System.now().toEpochMilliseconds()
            val durationSeconds = ((currentState.configuredTime - currentState.currentTime + currentState.overtimeTime) / 1000).toInt()
            val round = Round(
                id = currentTime.toString(),
                duration = durationSeconds,
                overtime = currentState.overtimeSeconds,
                timestamp = currentTime,
                gameId = currentState.activeGameId!!
            )

            val newRounds = currentState.rounds + round
            _state.value = currentState.copy(
                isRunning = false,
                isOvertime = false,
                currentTime = currentState.configuredTime,
                overtimeTime = 0L,
                rounds = newRounds
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
        val newRounds = currentState.rounds.filter { it.id != roundId }
        _state.value = currentState.copy(rounds = newRounds)

        // Save to storage asynchronously
        viewModelScope.launch {
            try {
                storage.saveRounds(newRounds)
            } catch (e: Exception) {
                // Storage save failed, but continue with UI update
            }
        }
    }

    fun resetHistoryForGame(gameId: String) {
        val currentState = _state.value
        val newRounds = currentState.rounds.filter { it.gameId != gameId }
        _state.value = currentState.copy(rounds = newRounds)

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

    fun createNewGame(name: String = "") {
        val newGame = Game(id = Clock.System.now().toEpochMilliseconds().toString(), date = getCurrentDate(), name = name)
        val newGames = (_state.value.games + newGame).sortedByDescending { it.id }
        _state.value = _state.value.copy(games = newGames, activeGameId = newGame.id)
        viewModelScope.launch {
            storage.saveGames(newGames)
        }
    }

    fun setActiveGame(gameId: String) {
        _state.value = _state.value.copy(activeGameId = gameId)
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
        viewModelScope.launch {
            storage.saveGames(updatedGames)
        }
    }

    fun deleteGame(gameId: String) {
        val currentState = _state.value
        val newGames = currentState.games.filter { it.id != gameId }.sortedByDescending { it.id }
        val newActiveGameId = if (currentState.activeGameId == gameId) {
            newGames.firstOrNull()?.id
        } else {
            currentState.activeGameId
        }
        _state.value = currentState.copy(games = newGames, activeGameId = newActiveGameId)
        viewModelScope.launch {
            storage.saveGames(newGames)
            storage.saveActiveGameId(newActiveGameId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        audioJob?.cancel()
    }
}