package net.solvetheriddle.roundtimer.model

data class TimerState(
    val configuredTime: Long = 150000L,    // User-selected duration in milliseconds (default 2:30)
    val currentTime: Long = 150000L,       // Current countdown value in milliseconds
    val overtimeTime: Long = 0L,           // Overtime counter in milliseconds
    val isRunning: Boolean = false,        // Timer active state
    val isOvertime: Boolean = false,       // Overtime mode flag
    val rounds: List<Round> = emptyList()  // Historical rounds array
) {
    // Convenience properties to get time values in seconds for display
    val configuredSeconds: Int get() = (configuredTime / 1000).toInt()
    val currentSeconds: Int get() = (currentTime / 1000).toInt()
    val overtimeSeconds: Int get() = (overtimeTime / 1000).toInt()
}
