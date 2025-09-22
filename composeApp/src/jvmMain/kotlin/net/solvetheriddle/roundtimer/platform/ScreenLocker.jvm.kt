package net.solvetheriddle.roundtimer.platform

actual class ScreenLocker {
    actual fun lock() {
        // No-op for JVM
    }

    actual fun unlock() {
        // No-op for JVM
    }
}

actual fun getScreenLocker(): ScreenLocker = ScreenLocker()