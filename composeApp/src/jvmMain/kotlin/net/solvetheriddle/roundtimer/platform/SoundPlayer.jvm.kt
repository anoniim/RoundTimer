package net.solvetheriddle.roundtimer.platform

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.solvetheriddle.roundtimer.model.Sound
import roundtimer.composeapp.generated.resources.Res
import javax.sound.sampled.AudioSystem
import java.io.ByteArrayInputStream

actual class SoundPlayer {
    actual fun playSound(sound: Sound) {
        GlobalScope.launch {
            try {
                val soundBytes = Res.readBytes("files/${sound.fileName}")
                val audioInputStream = AudioSystem.getAudioInputStream(ByteArrayInputStream(soundBytes))
                val clip = AudioSystem.getClip()
                clip.open(audioInputStream)
                clip.start()
            } catch (e: Exception) {
                println("Error playing sound: ${e.message}")
            }
        }
    }
}

actual fun getSoundPlayer(): SoundPlayer = SoundPlayer()