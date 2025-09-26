package net.solvetheriddle.roundtimer.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

@Composable
fun rememberIsLandscape(): Boolean {
    val windowSize = LocalWindowInfo.current.containerSize
    return windowSize.width > windowSize.height
}
