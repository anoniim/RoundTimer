package net.solvetheriddle.roundtimer.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.solvetheriddle.roundtimer.model.AudioPattern
import net.solvetheriddle.roundtimer.model.ScheduledSound
import net.solvetheriddle.roundtimer.model.Sound
import net.solvetheriddle.roundtimer.platform.SoundPlayer
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

/**
 * Handles precise audio scheduling with millisecond accuracy.
 * Separates audio timing from UI updates for better precision.
 */
@OptIn(ExperimentalTime::class)
class AudioScheduler(
    private val soundPlayer: SoundPlayer,
    private val scope: CoroutineScope
) {
    private var scheduledSounds = mutableListOf<ScheduledSound>()
    private var schedulerJob: Job? = null
    private var currentSoundJob: Job? = null // Track currently playing sound
    private var startTime: TimeSource.Monotonic.ValueTimeMark? = null
    private var timeOffset: Long = 0L // Offset for fast-forward adjustments
    
    /**
     * Start the audio scheduler with a list of pre-calculated sound events
     */
    fun start(timerDurationMs: Long, audioEvents: List<ScheduledSound>) {
        stop() // Cancel any existing scheduler
        
        scheduledSounds.clear()
        scheduledSounds.addAll(audioEvents.sortedBy { it.triggerTimeMs })
        
        startTime = TimeSource.Monotonic.markNow()
        timeOffset = 0L
        
        
        schedulerJob = scope.launch {
            processScheduledSounds()
        }
    }
    
    /**
     * Add a sound event dynamically (useful for overtime sounds)
     */
    fun scheduleSound(delayMs: Long, sound: Sound, pattern: AudioPattern = AudioPattern.Single) {
        val triggerTime = getCurrentElapsedTime() + delayMs
        val scheduledSound = ScheduledSound(triggerTime, sound, pattern)
        
        synchronized(scheduledSounds) {
            scheduledSounds.add(scheduledSound)
            scheduledSounds.sortBy { it.triggerTimeMs }
        }
        
        // If delay is 0 (immediate), interrupt current sound and play now
        if (delayMs == 0L) {
            currentSoundJob?.cancel()
            soundPlayer.stopSound()
            
            // Remove from scheduled list since we're playing it now
            synchronized(scheduledSounds) {
                scheduledSounds.removeAll { it.triggerTimeMs == triggerTime && it.sound == sound }
            }
            
            currentSoundJob = scope.launch {
                playScheduledSound(scheduledSound)
            }
        }
    }
    
    /**
     * Fast forward the timer by the specified amount of milliseconds.
     * This will trigger any audio cues that should have played during the skipped time.
     */
    fun fastForward(fastForwardMs: Long) {
        timeOffset += fastForwardMs

        // Immediately trigger any sounds that should have played during the fast-forward period
        val currentTime = getCurrentElapsedTime()

        synchronized(scheduledSounds) {
            scheduledSounds.removeAll { it.triggerTimeMs <= currentTime }
            scheduledSounds.forEach {
                it.triggerTimeMs -= currentTime
            }
        }

        // Restart the processing loop to recalculate delays
        schedulerJob?.cancel()
        startTime = TimeSource.Monotonic.markNow()
        timeOffset = 0
        schedulerJob = scope.launch {
            processScheduledSounds()
        }
    }
    
    /**
     * Stop the scheduler and cancel all pending sounds
     */
    fun stop() {
        schedulerJob?.cancel()
        schedulerJob = null
        currentSoundJob?.cancel()
        currentSoundJob = null
        startTime = null
        timeOffset = 0L
        soundPlayer.stopSound()
    }
    
    /**
     * Get the current elapsed time including any fast-forward offsets
     */
    private fun getCurrentElapsedTime(): Long {
        return (startTime?.elapsedNow()?.inWholeMilliseconds ?: 0L) + timeOffset
    }
    
    private suspend fun processScheduledSounds() {
        while (scheduledSounds.isNotEmpty()) {
            // Check if we've been cancelled
            if (schedulerJob?.isCancelled == true) {
                break
            }
            
            val currentTime = getCurrentElapsedTime()
            val nextSound = scheduledSounds.firstOrNull()
            
            if (nextSound != null && currentTime >= nextSound.triggerTimeMs) {
                // Remove and play the sound
                synchronized(scheduledSounds) {
                    scheduledSounds.removeFirst()
                }
                
//                // Cancel any currently playing sound before starting new one
//                currentSoundJob?.cancel()
//                soundPlayer.stopSound()
                
                // Start the new sound
                currentSoundJob = scope.launch {
                    playScheduledSound(nextSound)
                }
            } else {
                // Calculate precise delay until next sound
                val delayMs = if (nextSound != null) {
                    maxOf(1L, nextSound.triggerTimeMs - currentTime)
                } else {
                    100L // Default check interval if no sounds pending
                }
                delay(delayMs)
            }
        }
    }
    
    private suspend fun playScheduledSound(scheduledSound: ScheduledSound) {
        try {
            when (val pattern = scheduledSound.pattern) {
                is AudioPattern.Single -> {
                    soundPlayer.playSound(scheduledSound.sound)
                }
                
                is AudioPattern.Repeated -> {
                    repeat(pattern.count) { index ->
                        soundPlayer.playSound(scheduledSound.sound)
                        if (index < pattern.count - 1) {
                            delay(pattern.intervalMs)
                        }
                    }
                }
                
                is AudioPattern.Custom -> {
                    soundPlayer.playSound(scheduledSound.sound)
                    pattern.schedule.forEach { delayMs ->
                        delay(delayMs)
                        soundPlayer.playSound(scheduledSound.sound)
                    }
                }
            }
        } finally {
            // Clear the current sound job when this sound completes (naturally or by cancellation)
            if (currentSoundJob?.isActive != true) {
                currentSoundJob = null
            }
        }
    }
}