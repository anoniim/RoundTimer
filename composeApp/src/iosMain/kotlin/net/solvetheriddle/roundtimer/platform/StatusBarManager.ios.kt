package net.solvetheriddle.roundtimer.platform

import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyle

actual class StatusBarManager {
    actual fun setStatusBarStyle(isDarkContent: Boolean) {
        val style = if (isDarkContent) {
            UIStatusBarStyle.UIStatusBarStyleDarkContent
        } else {
            UIStatusBarStyle.UIStatusBarStyleLightContent
        }
        
        UIApplication.sharedApplication.setStatusBarStyle(style, animated = true)
    }
}

actual fun getStatusBarManager(): StatusBarManager = StatusBarManager()