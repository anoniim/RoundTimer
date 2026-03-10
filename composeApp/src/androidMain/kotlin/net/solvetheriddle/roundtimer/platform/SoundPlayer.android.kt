package net.solvetheriddle.roundtimer.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    OVERTIME_CAS_VYPRSEL(R.raw.overtime_cas_vyprsel),
    OVERTIME_JONE_JEDEM(R.raw.overtime_jone_jedem),
    OVERTIME_JONE_POD(R.raw.overtime_jone_pod),
    OVERTIME_SEBEDESTRUKCE(R.raw.overtime_sebedestrukce),
    OVERTIME_SUP_SUP_SUP(R.raw.overtime_sup_sup_sup),
    OVERTIME_TAK_ALE_UZ(R.raw.overtime_tak_ale_uz),
    OVERTIME_HELE_TY(R.raw.overtime_hele_ty),
    OVERTIME_KARTY_VSICHNI(R.raw.overtime_karty_vsichni_zahraji_ted),
    OVERTIME_KONEC_KARTU(R.raw.overtime_konec_kartu_zahrat_ted_hned),
    OVERTIME_MACKU_NOTAK(R.raw.overtime_macku_notak),
    OVERTIME_NA_KOHO_DO_PULNOCI(R.raw.overtime_na_koho_to_cekame_do_pulnoci),
    OVERTIME_NA_KOHO_DO_ZEJTRA(R.raw.overtime_na_koho_to_cekame_do_zejtra),
    OVERTIME_NA_KOHO_CEKAME(R.raw.overtime_na_koho_to_cekame),
    OVERTIME_TAK_NA_KOHO(R.raw.overtime_tak_na_koho_se_ceka),
    OVERTIME_TAK_POJD(R.raw.overtime_tak_pojd_dej_to_tam),
    OVERTIME_TAKOVA_DOBA(R.raw.overtime_takova_doba_to_neni_mozny),
    OVERTIME_TENHLE_TAH(R.raw.overtime_tenhle_tah_je_u_konce_pokracujem),
    OVERTIME_TEZKEJ_VYBER(R.raw.overtime_tezkej_vyber_co),
    OVERTIME_TO_JE_DOBA(R.raw.overtime_to_je_doba),
    OVERTIME_TRI_DVA_JEDNA(R.raw.overtime_tri_dva_jedna_konec),
    OVERTIME_UZ_JSME_CEKALI(R.raw.overtime_uz_jsme_cekali_dost_dlouho),
    OVERTIME_ZAHRAJ_SRDICKEM(R.raw.overtime_zahraj_to_srdickem),
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
    
    actual fun cleanup() {
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