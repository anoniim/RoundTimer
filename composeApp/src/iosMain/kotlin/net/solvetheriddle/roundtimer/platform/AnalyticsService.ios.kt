package net.solvetheriddle.roundtimer.platform

actual class AnalyticsService {
    actual fun logEvent(name: String, params: Map<String, String>?) {
        println("Analytics Event (iOS): $name, Params: $params")
    }
}

actual fun getAnalyticsService(): AnalyticsService = AnalyticsService()