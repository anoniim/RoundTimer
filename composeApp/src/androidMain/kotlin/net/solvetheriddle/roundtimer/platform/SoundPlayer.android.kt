package net.solvetheriddle.roundtimer.platform

import android.content.Context
import android.media.MediaPlayer
import net.solvetheriddle.roundtimer.AppContext
import net.solvetheriddle.roundtimer.model.Sound

actual class SoundPlayer(private val context: Context) {
    actual fun playSound(sound: Sound) {
        try {
            val assetManager = context.assets
            val fileDescriptor = assetManager.openFd("composeResources/roundtimer.composeapp.generated.resources/files/${sound.fileName}")
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(fileDescriptor.fileDescriptor, fileDescriptor.startOffset, fileDescriptor.length)
            fileDescriptor.close()
            mediaPlayer.prepare()
            mediaPlayer.setOnCompletionListener { mp ->
                mp.release()
            }
            mediaPlayer.start()
        } catch (e: Exception) {
            println("Error playing sound: ${e.message}")
        }
    }
}

actual fun getSoundPlayer(): SoundPlayer {
    return SoundPlayer(AppContext.INSTANCE)
}