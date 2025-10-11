package net.solvetheriddle.roundtimer.platform

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings
import androidx.compose.runtime.remember

/**
 * Checks if the device is currently using 3-button navigation.
 *
 * This is not a Composable function because the navigation mode is unlikely to change
 * while the app is running, and reading it once is sufficient.
 *
 * @return `true` if 3-button navigation is enabled, `false` otherwise.
 */
@Composable
actual fun isButtonNavigationEnabled(): Boolean {
    val context = LocalContext.current
    // We use 'remember' because this value is not expected to change while the app is running.
    // This avoids repeatedly querying the system settings on every recomposition.
    return remember {
        // A value of 0 indicates 3-button navigation mode.
        // A value of 1 indicates 2-button navigation mode (pill).
        // A value of 2 indicates gesture navigation mode.
        Settings.Secure.getInt(context.contentResolver, "navigation_mode", 0) == 0
    }
}