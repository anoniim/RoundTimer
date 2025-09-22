package net.solvetheriddle.roundtimer.platform

expect class ScreenLocker {
    fun lock()
    fun unlock()
}

expect fun getScreenLocker(): ScreenLocker
