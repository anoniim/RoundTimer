package net.solvetheriddle.roundtimer.platform

actual class AnalyticsService {
    actual fun logEvent(name: String, params: Map<String, String>?) {
        println("Analytics Event (JVM): $name, Params: $params")
    }
}

actual fun getAnalyticsService(): AnalyticsService = AnalyticsService()