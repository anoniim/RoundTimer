package net.solvetheriddle.roundtimer.platform

import android.content.Context
import android.media.SoundPool
import android.media.AudioAttributes
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.solvetheriddle.roundtimer.AppContext
import net.solvetheriddle.roundtimer.model.Sound
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance Android sound player using SoundPool for efficient audio playback
 * Pre-loads all sounds for instant playback without I/O lag
 */
actual class SoundPlayer(private val context: Context) {
    private var soundPool: SoundPool? = null
    private val soundIds = ConcurrentHashMap<Sound, Int>()
    private var currentStreamId: Int = 0
    
    companion object {
        private var isInitialized = false
        private val initMutex = Mutex()
        private const val MAX_STREAMS = 5 // Allow multiple overlapping sounds
    }

    init {
        // Initialize SoundPool and pre-load all sounds asynchronously
        GlobalScope.launch(Dispatchers.IO) {
            initializeAudioSystem()
        }
    }
    
    private suspend fun initializeAudioSystem() {
        initMutex.withLock {
            if (isInitialized) return
            
            try {
                // Create SoundPool with optimized settings for rapid playback
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                
                soundPool = SoundPool.Builder()
                    .setMaxStreams(MAX_STREAMS)
                    .setAudioAttributes(audioAttributes)
                    .build()
                
                // Pre-load all sound files
                Sound.values().forEach { sound ->
                    preloadSound(sound)
                }
                
                isInitialized = true
                println("Android audio system initialized with ${soundIds.size} sounds")
            } catch (e: Exception) {
                println("Failed to initialize Android audio system: ${e.message}")
            }
        }
    }
    
    private fun preloadSound(sound: Sound) {
        try {
            val assetManager = context.assets
            val fileDescriptor = assetManager.openFd(
                "composeResources/roundtimer.composeapp.generated.resources/files/${sound.fileName}"
            )
            
            val soundId = soundPool?.load(
                fileDescriptor.fileDescriptor,
                fileDescriptor.startOffset,
                fileDescriptor.length,
                1 // Priority
            ) ?: return
            
            soundIds[sound] = soundId
            fileDescriptor.close()
        } catch (e: Exception) {
            println("Failed to preload sound ${sound.fileName}: ${e.message}")
        }
    }

    actual fun playSound(sound: Sound) {
        // Non-blocking audio playback
        GlobalScope.launch {
            playAudioAsync(sound)
        }
    }
    
    private suspend fun playAudioAsync(sound: Sound) {
        try {
            // Wait for audio system to be initialized
            if (!isInitialized) {
                initializeAudioSystem()
            }
            
            val soundId = soundIds[sound] ?: return
            
            // Stop previous sound before playing new one
            if (currentStreamId != 0) {
                soundPool?.stop(currentStreamId)
            }
            
            // Play the sound with optimized parameters
            currentStreamId = soundPool?.play(
                soundId,
                1.0f, // Left volume
                1.0f, // Right volume
                1,    // Priority
                0,    // Loop (0 = no loop)
                1.0f  // Rate (normal speed)
            ) ?: 0
        } catch (e: Exception) {
            println("Error playing sound ${sound.fileName}: ${e.message}")
        }
    }

    actual fun stopSound() {
        try {
            if (currentStreamId != 0) {
                soundPool?.stop(currentStreamId)
                currentStreamId = 0
            }
        } catch (e: Exception) {
            println("Error stopping sound: ${e.message}")
        }
    }
    
    fun cleanup() {
        try {
            soundPool?.release()
            soundPool = null
            soundIds.clear()
            currentStreamId = 0
        } catch (e: Exception) {
            println("Error during cleanup: ${e.message}")
        }
    }
}

actual fun getSoundPlayer(): SoundPlayer {
    return SoundPlayer(AppContext.INSTANCE)
}