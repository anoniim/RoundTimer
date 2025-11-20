package net.solvetheriddle.roundtimer.model

import kotlinx.serialization.Serializable

@Serializable
data class Game(
    val id: String, // Unique ID
    val date: String, // Date string
    val name: String = "",
    val customTypes: List<String> = emptyList(),
    val playerTypes: List<String> = emptyList(),
    val typeConfigurations: Map<String, Long> = emptyMap() // Map of Type Name -> Configured Time (millis)
)
