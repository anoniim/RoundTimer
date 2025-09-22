package net.solvetheriddle.roundtimer.platform

import platform.UIKit.UIApplication

actual class ScreenLocker {
    actual fun lock() {
        UIApplication.sharedApplication.idleTimerDisabled = true
    }

    actual fun unlock() {
        UIApplication.sharedApplication.idleTimerDisabled = false
    }
}

actual fun getScreenLocker(): ScreenLocker = ScreenLocker()