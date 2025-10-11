package net.solvetheriddle.roundtimer.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.solvetheriddle.roundtimer.AppContext
import net.solvetheriddle.roundtimer.R
import net.solvetheriddle.roundtimer.model.Sound
import java.util.concurrent.ConcurrentHashMap

enum class AndroidSound(val resourceId: Int) {
    DUM(R.raw.dum),
    CALL(R.raw.call),
    ALMOST(R.raw.almost),
    INTENSE(R.raw.intense),
    TIMEOUT_GONG(R.raw.timeout_gong),
    OVERTIME(R.raw.overtime_beat_alarm),
    OVERTIME_CALL1(R.raw.overtime_cas_vyprsel),
    OVERTIME_CALL2(R.raw.overtime_jone_jedem),
    OVERTIME_CALL3(R.raw.overtime_jone_pod),
    OVERTIME_CALL4(R.raw.overtime_sebedestrukce),
    OVERTIME_CALL5(R.raw.overtime_sup_sup_sup),
    OVERTIME_CALL6(R.raw.overtime_tak_ale_uz)
}

/**
 * High-performance Android sound player using SoundPool for efficient audio playback
 * Pre-loads all sounds for instant playback without I/O lag
 */
actual class SoundPlayer(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var soundPool: SoundPool? = null
    private var mediaPlayer: MediaPlayer? = null
    private val soundIds = ConcurrentHashMap<Sound, Int>()
    private val activeStreams = mutableListOf<Int>()
    private var soundsLoaded = false
    private val soundsToLoad: Int = Sound.entries.size
    private var pendingSound: Sound? = null

    companion object {
        private const val MAX_STREAMS = 5 // Allow multiple overlapping sounds
    }

    init {
        initializeAudioSystem()
    }

    private fun initializeAudioSystem() {
        println("Initializing audio system")
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(audioAttributes)
                .build()

            var loadedCount = 0
            soundPool?.setOnLoadCompleteListener { _, _, status ->
                if (status == 0) {
                    loadedCount++
                    if (loadedCount >= soundsToLoad) {
                        soundsLoaded = true
                        println("All sounds loaded successfully")
                        pendingSound?.let {
                            scope.launch {
                                playSound(it)
                                pendingSound = null
                            }
                        }
                    }
                } else {
                    println("Failed to load sound, status: $status")
                }
            }

            Sound.entries.forEach { sound ->
                preloadSound(sound)
            }

            println("Android audio system initialized. Loading ${soundsToLoad} sounds.")
        } catch (e: Exception) {
            println("Failed to initialize Android audio system: ${e.message}")
        }
    }
    
    private fun preloadSound(sound: Sound) {
        try {
            val androidSound = AndroidSound.valueOf(sound.name)
            val soundId = soundPool?.load(context, androidSound.resourceId, 1) ?: return
            soundIds[sound] = soundId
        } catch (e: Exception) {
            println("Failed to preload sound ${sound.fileName}: ${e.message}")
        }
    }

    actual fun playSound(sound: Sound) {
        if (sound == Sound.INTENSE) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, R.raw.intense)
            mediaPlayer?.setOnErrorListener { _, _, _ ->
                println("Error playing sound ${sound.fileName}")
                false
            }
            mediaPlayer?.start()
        } else {
            if (!soundsLoaded) {
                pendingSound = sound
                println("Sounds not loaded yet, queuing sound: ${sound.fileName}")
                return
            }

            try {
                val soundId = soundIds[sound] ?: return

                val streamId = soundPool?.play(
                    soundId,
                    1.0f, // Left volume
                    1.0f, // Right volume
                    1,    // Priority
                    0,    // Loop (0 = no loop)
                    1.0f  // Rate (normal speed)
                )
                streamId?.let { activeStreams.add(it) }
            } catch (e: Exception) {
                println("Error playing sound ${sound.fileName}: ${e.message}")
            }
        }
    }

    actual fun stopSound() {
        try {
            activeStreams.forEach { soundPool?.stop(it) }
            activeStreams.clear()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            println("Error stopping sound: ${e.message}")
        }
    }
    
    fun cleanup() {
        try {
            scope.cancel()
            soundPool?.release()
            soundPool = null
            soundIds.clear()
            activeStreams.clear()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            println("Error during cleanup: ${e.message}")
        }
    }
}

actual fun getSoundPlayer(): SoundPlayer {
    return SoundPlayerSingleton.INSTANCE
}

private object SoundPlayerSingleton {
    val INSTANCE: SoundPlayer by lazy { SoundPlayer(AppContext.INSTANCE) }
}