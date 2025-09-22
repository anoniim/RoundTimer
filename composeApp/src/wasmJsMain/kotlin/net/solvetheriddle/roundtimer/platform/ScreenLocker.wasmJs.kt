package net.solvetheriddle.roundtimer.platform

import kotlinx.browser.window

actual class ScreenLocker {
    private var wakeLock: Any? = null

    actual fun lock() {
        try {
            val navigator = window.navigator.asDynamic()
            if (js("typeof navigator.wakeLock !== 'undefined'") as Boolean) {
                navigator.wakeLock.request("screen").then {
                    wakeLock = it
                }
            }
        } catch (e: Exception) {
            println("Screen Wake Lock API not supported or failed: ${e.message}")
        }
    }

    actual fun unlock() {
        (wakeLock as? dynamic)?.release()
    }
}

actual fun getScreenLocker(): ScreenLocker = ScreenLocker()