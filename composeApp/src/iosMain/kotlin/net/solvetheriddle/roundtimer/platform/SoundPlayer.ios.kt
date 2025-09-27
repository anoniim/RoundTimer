package net.solvetheriddle.roundtimer.platform

import platform.AVFoundation.AVAudioPlayer
import platform.AVFoundation.AVAudioSession
import platform.AVFoundation.AVAudioSessionCategoryPlayback
import platform.AVFoundation.setCategory
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import net.solvetheriddle.roundtimer.model.Sound

actual class SoundPlayer {
    private var player: AVAudioPlayer? = null

    actual fun playSound(sound: Sound) {
        val soundUrl = NSBundle.mainBundle.URLForResource("files/${sound.fileName.removeSuffix(".wav")}", withExtension = "wav")
        if (soundUrl != null) {
            try {
                AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, error = null)
                player = AVAudioPlayer(contentsOfURL = soundUrl, error = null)
                player?.play()
            } catch (e: Exception) {
                println("Error playing sound: ${e.message}")
            }
        }
    }
}

actual fun getSoundPlayer(): SoundPlayer = SoundPlayer()