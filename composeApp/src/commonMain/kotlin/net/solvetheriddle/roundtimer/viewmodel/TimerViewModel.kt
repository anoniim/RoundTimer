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
import net.solvetheriddle.roundtimer.model.AudioCue
import net.solvetheriddle.roundtimer.model.Sound
import net.solvetheriddle.roundtimer.model.Round
import net.solvetheriddle.roundtimer.model.TimerState
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
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    init {
        // Initialize storage and load saved rounds
        try {
            storage.initialize()
            viewModelScope.launch {
                try {
                    val savedRounds = storage.loadRounds()
                    _state.value = _state.value.copy(rounds = savedRounds)
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
        AudioCue(threshold = 45, sound = Sound.CALL),
        AudioCue(threshold = 42, sound = Sound.CALL),
        AudioCue(threshold = 39, sound = Sound.CALL),
        AudioCue(threshold = 36, sound = Sound.INTENSE),
        AudioCue(threshold = 18, sound = Sound.INTENSE),
    )

    fun updateConfiguredTime(seconds: Int) {
        if (!_state.value.isRunning) {
            val milliseconds = seconds * 1000L
            _state.value = _state.value.copy(
                configuredTime = milliseconds,
                currentTime = milliseconds
            )
        }
    }

    fun startTimer() {
        val currentState = _state.value
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
                timestamp = currentTime
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

    fun resetHistory() {
        _state.value = _state.value.copy(rounds = emptyList())

        // Clear storage asynchronously
        viewModelScope.launch {
            try {
                storage.clearAll()
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

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        audioJob?.cancel()
    }
}