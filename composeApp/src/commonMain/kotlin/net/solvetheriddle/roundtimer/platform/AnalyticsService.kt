package net.solvetheriddle.roundtimer.platform

expect class AnalyticsService {
    fun logEvent(name: String, params: Map<String, String>? = null)
}

expect fun getAnalyticsService(): AnalyticsService
