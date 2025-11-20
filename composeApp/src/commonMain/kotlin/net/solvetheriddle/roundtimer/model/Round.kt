package net.solvetheriddle.roundtimer.model

import kotlinx.serialization.Serializable

@Serializable
data class Round(
    val id: String,           // Unique identifier (timestamp-based)
    val duration: Int,        // Actual round duration in seconds
    val overtime: Int,        // Overtime seconds (0 if none)
    val timestamp: Long,      // When round was completed (epoch millis)
    val gameId: String,       // ID of the game this round belongs to
    val category: String = "Everyone", // Deprecated: kept for backward compatibility
    val phase: String = "Setup",       // New: Phase of the round
    val player: String = "Everyone"    // New: Player of the round
)
