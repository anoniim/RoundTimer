package net.solvetheriddle.roundtimer

import androidx.compose.runtime.Composable

@Composable
actual fun BackPressHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op for JS
}
