package net.solvetheriddle.roundtimer.model

data class AudioCue(
    val threshold: Int,                        // Seconds remaining
    val beatPattern: BeatPattern               // Single or double beats
)

enum class BeatPattern {
    SINGLE,
    DOUBLE
}