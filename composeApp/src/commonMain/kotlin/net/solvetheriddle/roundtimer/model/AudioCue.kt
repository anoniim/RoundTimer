package net.solvetheriddle.roundtimer.model

data class AudioCue(
    val threshold: Int,                        // Seconds remaining
    val sound: Sound               // Single or double beats
)

enum class Sound(val fileName: String) {
    DUM("dum.wav"),
    CALL("call.wav"),
    ALMOST("almost.wav"),
    INTENSE("intense.wav"),
    OVERTIME("overtime_beat.wav"),
}