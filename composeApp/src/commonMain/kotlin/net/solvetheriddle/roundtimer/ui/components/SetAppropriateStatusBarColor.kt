package net.solvetheriddle.roundtimer.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import net.solvetheriddle.roundtimer.platform.getStatusBarManager

@Composable
internal fun SetAppropriateStatusBarColor() {
    val isDarkTheme = isSystemInDarkTheme()
    LaunchedEffect(isDarkTheme) {
        try {
            getStatusBarManager().setStatusBarStyle(isDarkContent = !isDarkTheme)
        } catch (e: Exception) {
            println("! This platform doesn't support status bar styling")
        }
    }
}