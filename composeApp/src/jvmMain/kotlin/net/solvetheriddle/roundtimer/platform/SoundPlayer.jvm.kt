package net.solvetheriddle.roundtimer.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.solvetheriddle.roundtimer.model.Sound
import roundtimer.composeapp.generated.resources.Res
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.*

/**
 * Optimized audio clip with reuse capabilities
 */
data class AudioClip(
    val clip: Clip,
    var isInUse: Boolean = false
)

/**
 * High-performance sound player with pre-loaded audio pools
 * Eliminates I/O operations during playback for smooth performance
 */
actual class SoundPlayer {
    private val audioCache = ConcurrentHashMap<Sound, ByteArray>()
    private val clipPools = ConcurrentHashMap<Sound, MutableList<AudioClip>>()
    private val poolMutex = ConcurrentHashMap<Sound, Mutex>()
    private var currentClip: AudioClip? = null
    
    companion object {
        private const val POOL_SIZE = 3 // Multiple clips per sound for overlapping playback
        private var isInitialized = false
        private val initMutex = Mutex()
    }

    init {
        // Pre-load all audio files asynchronously
        GlobalScope.launch(Dispatchers.IO) {
            initializeAudioSystem()
        }
    }
    
    private suspend fun initializeAudioSystem() {
        initMutex.withLock {
            if (isInitialized) return
            
            try {
                Sound.values().forEach { sound ->
                    poolMutex[sound] = Mutex()
                    preloadSound(sound)
                }
                isInitialized = true
                println("Audio system initialized with ${audioCache.size} sounds")
            } catch (e: Exception) {
                println("Failed to initialize audio system: ${e.message}")
            }
        }
    }
    
    private suspend fun preloadSound(sound: Sound) {
        try {
            // Load audio bytes once and cache them
            val soundBytes = Res.readBytes("files/${sound.fileName}")
            audioCache[sound] = soundBytes
            
            // Create a pool of pre-prepared clips
            val clips = mutableListOf<AudioClip>()
            repeat(POOL_SIZE) {
                val audioInputStream = AudioSystem.getAudioInputStream(
                    ByteArrayInputStream(soundBytes)
                )
                val clip = AudioSystem.getClip()
                clip.open(audioInputStream)
                audioInputStream.close()
                
                // Set up completion listener to mark clip as available
                clip.addLineListener { event ->
                    if (event.type == LineEvent.Type.STOP) {
                        GlobalScope.launch {
                            releaseClip(sound, AudioClip(clip, false))
                        }
                    }
                }
                
                clips.add(AudioClip(clip, false))
            }
            clipPools[sound] = clips
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
            val clip = acquireClip(sound) ?: return
            currentClip = clip
            
            // Reset clip position and play
            clip.clip.framePosition = 0
            clip.clip.start()
        } catch (e: Exception) {
            println("Error playing sound ${sound.fileName}: ${e.message}")
        }
    }
    
    private suspend fun acquireClip(sound: Sound): AudioClip? {
        // Wait for audio system to be initialized
        if (!isInitialized) {
            initializeAudioSystem()
        }
        
        val mutex = poolMutex[sound] ?: return null
        val pool = clipPools[sound] ?: return null
        
        return mutex.withLock {
            // Find an available clip
            pool.firstOrNull { !it.isInUse }?.let { availableClip ->
                availableClip.isInUse = true
                availableClip
            }
        }
    }
    
    private suspend fun releaseClip(sound: Sound, clipToRelease: AudioClip) {
        val mutex = poolMutex[sound] ?: return
        val pool = clipPools[sound] ?: return
        
        mutex.withLock {
            pool.find { it.clip == clipToRelease.clip }?.let {
                it.isInUse = false
            }
        }
    }

    actual fun stopSound() {
        currentClip?.let { audioClip ->
            try {
                audioClip.clip.stop()
                audioClip.clip.framePosition = 0
                audioClip.isInUse = false
            } catch (e: Exception) {
                println("Error stopping sound: ${e.message}")
            }
        }
        currentClip = null
    }
    
    actual fun cleanup() {
        clipPools.values.forEach { pool ->
            pool.forEach { audioClip ->
                try {
                    audioClip.clip.close()
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
        clipPools.clear()
        audioCache.clear()
    }
}

actual fun getSoundPlayer(): SoundPlayer = SoundPlayer()