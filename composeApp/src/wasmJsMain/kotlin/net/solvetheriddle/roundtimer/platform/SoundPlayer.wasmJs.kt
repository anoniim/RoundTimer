package net.solvetheriddle.roundtimer.platform

import net.solvetheriddle.roundtimer.model.Sound
import org.w3c.dom.Audio

actual class SoundPlayer {
    private var audio: Audio? = null

    actual fun playSound(sound: Sound) {
        try {
            audio?.pause()
            audio = Audio("files/${sound.fileName}")
            audio?.play()
        } catch (e: Exception) {
            println("Error playing sound: ${e.message}")
        }
    }

    actual fun stopSound() {
        audio?.pause()
    }
}

actual fun getSoundPlayer(): SoundPlayer = SoundPlayer()