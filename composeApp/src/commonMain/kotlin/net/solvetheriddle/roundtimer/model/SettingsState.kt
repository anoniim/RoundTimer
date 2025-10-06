package net.solvetheriddle.roundtimer.model

import kotlinx.serialization.Serializable

@Serializable
data class SettingsState(
    val isSubtleDrummingEnabled: Boolean = true,
    val isIntenseDrummingEnabled: Boolean = true,
    val isOvertimeAlarmEnabled: Boolean = false,
    val isTimeoutGongEnabled: Boolean = true,
    val isJonasScoldingEnabled: Boolean = false,
    val isSecretFastForwardEnabled: Boolean = false
)
