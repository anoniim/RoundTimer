package net.solvetheriddle.roundtimer.platform

import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.setStatusBarStyle

actual class StatusBarManager {
    actual fun setStatusBarStyle(isDarkContent: Boolean) {
        val style = if (isDarkContent) {
            UIStatusBarStyleDarkContent
        } else {
            UIStatusBarStyleLightContent
        }
        UIApplication.sharedApplication.setStatusBarStyle(style, animated = false)
    }
}

actual fun getStatusBarManager(): StatusBarManager = StatusBarManager()