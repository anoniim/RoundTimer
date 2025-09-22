package net.solvetheriddle.roundtimer.platform

import android.view.WindowManager
import net.solvetheriddle.roundtimer.CurrentActivity

actual class ScreenLocker {
    actual fun lock() {
        CurrentActivity.INSTANCE?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    actual fun unlock() {
        CurrentActivity.INSTANCE?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

actual fun getScreenLocker(): ScreenLocker = ScreenLocker()