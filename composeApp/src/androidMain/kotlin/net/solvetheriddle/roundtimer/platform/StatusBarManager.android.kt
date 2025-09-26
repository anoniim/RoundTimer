package net.solvetheriddle.roundtimer.platform

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat

actual class StatusBarManager {
    companion object {
        private var activity: Activity? = null
        
        fun initialize(activity: Activity) {
            this.activity = activity
        }
    }
    
    actual fun setStatusBarStyle(isDarkContent: Boolean) {
        val currentActivity = activity ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) and above
            val windowInsetsController = currentActivity.window.insetsController
            if (isDarkContent) {
                windowInsetsController?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                windowInsetsController?.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        } else {
            // Below Android 11
            WindowCompat.setDecorFitsSystemWindows(currentActivity.window, false)
            val windowInsetsController = WindowCompat.getInsetsController(currentActivity.window, currentActivity.window.decorView)
            windowInsetsController.isAppearanceLightStatusBars = isDarkContent
        }
    }
}

actual fun getStatusBarManager(): StatusBarManager = StatusBarManager()
