package net.solvetheriddle.roundtimer.platform

import net.solvetheriddle.roundtimer.model.Sound
import org.w3c.dom.Audio

actual class SoundPlayer {
    actual fun playSound(sound: Sound) {
        try {
            Audio("files/${sound.fileName}").play()
        } catch (e: Exception) {
            println("Error playing sound: ${e.message}")
        }
    }
}

actual fun getSoundPlayer(): SoundPlayer = SoundPlayer()