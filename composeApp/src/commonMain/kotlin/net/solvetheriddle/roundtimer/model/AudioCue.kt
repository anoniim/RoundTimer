package net.solvetheriddle.roundtimer.model

/**
 * Represents a scheduled audio cue with precise timing
 */
data class AudioCue(
    val threshold: Int,                        // Seconds remaining when this cue should trigger
    val sound: Sound,                          // The sound to play
    val pattern: AudioPattern = AudioPattern.Single // How the sound should be played
)

/**
 * Represents a scheduled sound event with exact timing
 */
data class ScheduledSound(
    val triggerTimeMs: Long,                   // Exact millisecond when to trigger
    val sound: Sound,                          // The sound to play
    val pattern: AudioPattern = AudioPattern.Single
)

/**
 * Defines how audio cues should be played
 */
sealed class AudioPattern {
    data object Single : AudioPattern()
    data class Repeated(val count: Int, val intervalMs: Long) : AudioPattern()
    data class Custom(val schedule: List<Long>) : AudioPattern() // List of delays in ms
}

enum class Sound(val fileName: String) {
    DUM("dum.wav"),
    CALL("call.wav"),
    ALMOST("almost.wav"),
    INTENSE("intense.wav"),
    TIMEOUT_GONG("timeout_gong.wav"),
    OVERTIME("overtime_beat_alarm.wav"),
    OVERTIME_CALL1("overtime_cas_vyprsel.wav"),
    OVERTIME_CALL2("overtime_jone_jedem.wav"),
    OVERTIME_CALL3("overtime_jone_pod.wav"),
    OVERTIME_CALL4("overtime_sebedestrukce.wav"),
    OVERTIME_CALL5("overtime_sup_sup_sup.wav"),
    OVERTIME_CALL6("overtime_tak_ale_uz.wav"),
}
