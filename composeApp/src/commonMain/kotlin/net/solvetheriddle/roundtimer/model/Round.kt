package net.solvetheriddle.roundtimer.model

import kotlinx.serialization.Serializable

@Serializable
data class Round(
    val id: String,           // Unique identifier (timestamp-based)
    val duration: Int,        // Actual round duration in seconds
    val overtime: Int,        // Overtime seconds (0 if none)
    val timestamp: Long       // When round was completed (epoch millis)
)
