package net.solvetheriddle.roundtimer

import androidx.compose.runtime.Composable

@Composable
expect fun BackPressHandler(enabled: Boolean, onBack: () -> Unit)
