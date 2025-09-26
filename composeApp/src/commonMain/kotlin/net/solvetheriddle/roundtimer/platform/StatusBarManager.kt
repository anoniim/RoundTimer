package net.solvetheriddle.roundtimer.platform

expect class StatusBarManager {
    fun setStatusBarStyle(isDarkContent: Boolean)
}

expect fun getStatusBarManager(): StatusBarManager