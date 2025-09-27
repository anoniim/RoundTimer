package net.solvetheriddle.roundtimer.platform

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.solvetheriddle.roundtimer.model.Sound
import roundtimer.composeapp.generated.resources.Res
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import java.io.ByteArrayInputStream

actual class SoundPlayer {
    private var clip: Clip? = null

    actual fun playSound(sound: Sound) {
        GlobalScope.launch {
            try {
                val soundBytes = Res.readBytes("files/${sound.fileName}")
                val audioInputStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(soundBytes))
                clip?.close()
                clip = AudioSystem.getClip()
                clip?.open(audioInputStream)
                clip?.start()
            } catch (e: Exception) {
                println("Error playing sound: ${e.message}")
            }
        }
    }

    actual fun stopSound() {
        clip?.stop()
        clip?.close()
    }
}

actual fun getSoundPlayer(): SoundPlayer = SoundPlayer()