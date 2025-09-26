package net.solvetheriddle.roundtimer.platform

actual class StatusBarManager {
    actual fun setStatusBarStyle(isDarkContent: Boolean) {
        // No-op for JVM/Desktop
    }
}

actual fun getStatusBarManager(): StatusBarManager = StatusBarManager()