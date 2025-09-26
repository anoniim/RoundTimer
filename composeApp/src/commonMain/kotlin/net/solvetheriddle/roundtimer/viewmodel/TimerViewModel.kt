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
import net.solvetheriddle.roundtimer.model.BeatPattern
import net.solvetheriddle.roundtimer.model.Round
import net.solvetheriddle.roundtimer.model.TimerState
import net.solvetheriddle.roundtimer.storage.RoundTimerStorage
import net.solvetheriddle.roundtimer.storage.createPlatformStorage
import kotlin.time.ExperimentalTime

private const val UPDATE_INTERVAL = 50L

class TimerViewModel : ViewModel() {
    
    private val storage by lazy { 
        RoundTimerStorage(createPlatformStorage())
    }
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
        AudioCue(threshold = 30, beatPattern = BeatPattern.SINGLE),   // 30s: single beat every 5s
        AudioCue(threshold = 20, beatPattern = BeatPattern.SINGLE),   // 20s: single beat every 1s
        AudioCue(threshold = 15, beatPattern = BeatPattern.DOUBLE),   // 15s: double beat every 1s
        AudioCue(threshold = 10, beatPattern = BeatPattern.DOUBLE)    // 10s: 2 double beats per second
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
                    _state.value = currentState.copy(
                        currentTime = newTime
                    )
                    
                    // Handle audio cues (check every 100ms but only trigger on second boundaries)
                    val currentSeconds = (newTime / 1000).toInt()
                    val previousSeconds = (currentState.currentTime / 1000).toInt()
                    if (currentSeconds != previousSeconds) {
                        handleAudioCues(currentSeconds)
                        
                        // Check if time reached zero
                        if (currentSeconds == 0) {
                            playGongSound()
                        }
                    }
                } else {
                    // Overtime mode
                    _state.value = currentState.copy(
                        isOvertime = true,
                        overtimeTime = currentState.overtimeTime + UPDATE_INTERVAL
                    )
                }
            }
        }
    }
    
    private fun handleAudioCues(remainingSeconds: Int) {
        val applicableCue = audioCues.find { it.threshold == remainingSeconds }
        applicableCue?.let { cue ->
            when {
                cue.threshold == 30 -> playDrumBeat(cue.beatPattern, 0.2) // Every 5 seconds
                cue.threshold == 20 -> playDrumBeat(cue.beatPattern, 1.0) // Every second
                cue.threshold == 15 -> playDrumBeat(cue.beatPattern, 1.0) // Every second
                cue.threshold == 10 -> playDrumBeat(cue.beatPattern, 2.0) // 2 per second
            }
        }
        
        // Continue patterns
        when (remainingSeconds) {
            in 1..30 -> {
                if (remainingSeconds % 5 == 0 && remainingSeconds <= 30) {
                    playDrumBeat(BeatPattern.SINGLE, 0.2)
                }
            }
            in 1..20 -> playDrumBeat(BeatPattern.SINGLE, 1.0)
            in 1..15 -> playDrumBeat(BeatPattern.DOUBLE, 1.0)
            in 1..10 -> playDrumBeat(BeatPattern.DOUBLE, 2.0)
        }
    }
    
    private fun playDrumBeat(pattern: BeatPattern, frequency: Double) {
        // Placeholder for audio implementation
        // This would be platform-specific
    }
    
    private fun playGongSound() {
        // Placeholder for gong sound implementation
        // This would be platform-specific
    }
    
    @OptIn(ExperimentalTime::class)
    fun stopTimer() {
        timerJob?.cancel()
        audioJob?.cancel()
        
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