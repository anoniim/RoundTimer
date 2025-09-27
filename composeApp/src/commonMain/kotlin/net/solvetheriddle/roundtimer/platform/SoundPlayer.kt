package net.solvetheriddle.roundtimer.platform

import net.solvetheriddle.roundtimer.model.Sound

expect class SoundPlayer {
    fun playSound(sound: Sound)
}

expect fun getSoundPlayer(): SoundPlayer
