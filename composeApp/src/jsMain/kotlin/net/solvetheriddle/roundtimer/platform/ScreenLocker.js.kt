package net.solvetheriddle.roundtimer.platform

import kotlinx.browser.window

actual class ScreenLocker {
    private var wakeLock: dynamic = null

    actual fun lock() {
        try {
            window.navigator.asDynamic().wakeLock.request("screen").then {
                wakeLock = it
            }
        } catch (e: Exception) {
            println("Screen Wake Lock API not supported or failed: ${e.message}")
        }
    }

    actual fun unlock() {
        wakeLock?.release()
    }
}

actual fun getScreenLocker(): ScreenLocker = ScreenLocker()