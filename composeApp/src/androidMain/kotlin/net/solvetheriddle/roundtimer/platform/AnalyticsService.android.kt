package net.solvetheriddle.roundtimer.platform

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

actual class AnalyticsService() {
    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    actual fun logEvent(name: String, params: Map<String, String>?) {
        val bundle = params?.let { map ->
            Bundle().apply {
                map.forEach { (key, value) -> putString(key, value) }
            }
        }
        firebaseAnalytics.logEvent(name, bundle)
    }
}

actual fun getAnalyticsService(): AnalyticsService = AnalyticsService()