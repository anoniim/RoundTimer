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
    var triggerTimeMs: Long,                   // Exact millisecond when to trigger
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
    OVERTIME_CAS_VYPRSEL("overtime_cas_vyprsel.wav"),
    OVERTIME_JONE_JEDEM("overtime_jone_jedem.wav"),
    OVERTIME_JONE_POD("overtime_jone_pod.wav"),
    OVERTIME_SEBEDESTRUKCE("overtime_sebedestrukce.wav"),
    OVERTIME_SUP_SUP_SUP("overtime_sup_sup_sup.wav"),
    OVERTIME_TAK_ALE_UZ("overtime_tak_ale_uz.wav"),
    OVERTIME_HELE_TY("overtime_hele_ty.wav"),
    OVERTIME_KARTY_VSICHNI("overtime_karty_vsichni_zahraji_ted.wav"),
    OVERTIME_KONEC_KARTU("overtime_konec_kartu_zahrat_ted_hned.wav"),
    OVERTIME_MACKU_NOTAK("overtime_macku_notak.wav"),
    OVERTIME_NA_KOHO_DO_PULNOCI("overtime_na_koho_to_cekame_do_pulnoci.wav"),
    OVERTIME_NA_KOHO_DO_ZEJTRA("overtime_na_koho_to_cekame_do_zejtra.wav"),
    OVERTIME_NA_KOHO_CEKAME("overtime_na_koho_to_cekame.wav"),
    OVERTIME_TAK_NA_KOHO("overtime_tak_na_koho_se_ceka.wav"),
    OVERTIME_TAK_POJD("overtime_tak_pojd_dej_to_tam.wav"),
    OVERTIME_TAKOVA_DOBA("overtime_takova_doba_to_neni_mozny.wav"),
    OVERTIME_TENHLE_TAH("overtime_tenhle_tah_je_u_konce_pokracujem.wav"),
    OVERTIME_TEZKEJ_VYBER("overtime_tezkej_vyber_co.wav"),
    OVERTIME_TO_JE_DOBA("overtime_to_je_doba.wav"),
    OVERTIME_TRI_DVA_JEDNA("overtime_tri_dva_jedna_konec.wav"),
    OVERTIME_UZ_JSME_CEKALI("overtime_uz_jsme_cekali_dost_dlouho.wav"),
    OVERTIME_ZAHRAJ_SRDICKEM("overtime_zahraj_to_srdickem.wav");

    companion object {
        val overtimeFirstCall = listOf(
            OVERTIME_JONE_JEDEM,
            OVERTIME_JONE_POD,
            OVERTIME_KARTY_VSICHNI,
            OVERTIME_MACKU_NOTAK,
            OVERTIME_NA_KOHO_DO_PULNOCI,
            OVERTIME_NA_KOHO_DO_ZEJTRA,
            OVERTIME_NA_KOHO_CEKAME,
            OVERTIME_TAK_NA_KOHO,
            OVERTIME_TENHLE_TAH,
            OVERTIME_TEZKEJ_VYBER,
            OVERTIME_ZAHRAJ_SRDICKEM,
        )

        val overtimeSecondCall = listOf(
            OVERTIME_HELE_TY,
            OVERTIME_KONEC_KARTU,
            OVERTIME_CAS_VYPRSEL,
            OVERTIME_SEBEDESTRUKCE,
            OVERTIME_SUP_SUP_SUP,
            OVERTIME_TAK_ALE_UZ,
            OVERTIME_TAK_POJD,
            OVERTIME_TAKOVA_DOBA,
            OVERTIME_TO_JE_DOBA,
            OVERTIME_TRI_DVA_JEDNA,
            OVERTIME_UZ_JSME_CEKALI,
        )
    }
}
