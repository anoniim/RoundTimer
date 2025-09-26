package net.solvetheriddle.roundtimer.platform

actual class StatusBarManager {
    actual fun setStatusBarStyle(isDarkContent: Boolean) {
        // No-op for Web
    }
}

actual fun getStatusBarManager(): StatusBarManager = StatusBarManager()