package net.solvetheriddle.roundtimer.platform

import kotlinx.coroutines.runBlocking
import net.solvetheriddle.roundtimer.model.Sound
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource
import platform.AVFoundation.AVAudioPlayer
import platform.AVFoundation.AVAudioSession
import platform.AVFoundation.AVAudioSessionCategoryPlayback
import platform.AVFoundation.setCategory
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalResourceApi::class, ExperimentalForeignApi::class)
actual class SoundPlayer {
    private var player: AVAudioPlayer? = null

    actual fun playSound(sound: Sound) {
        try {
            val resource = resource("files/${sound.fileName}")
            val soundData = runBlocking { resource.readBytes() }

            val nsData = soundData.toNSData()

            AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, error = null)
            player?.stop()
            player = AVAudioPlayer(data = nsData, error = null)
            player?.play()
        } catch (e: Exception) {
            println("Error playing sound: ${e.message}")
        }
    }

    actual fun stopSound() {
        player?.stop()
    }
}

actual fun getSoundPlayer(): SoundPlayer = SoundPlayer()

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData {
    return this.usePinned {
        NSData.dataWithBytes(it.addressOf(0), this.size.toULong())
    }
}
