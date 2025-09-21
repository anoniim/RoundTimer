package net.solvetheriddle.roundtimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light theme colors
val LightPrimary = Color(0xffb96eea)
val LightPrimaryContainer = Color(0xffcfbfd1)
val LightSecondary = Color(0xffdacabb)
val LightSecondaryContainer = Color(0xfff8edde)
val LightBackground = Color(0xfff8d2b3)
val LightSurface = Color(0xfffff5d5)
val LightError = Color(0xFFB00020)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightOnSecondary = Color(0xFF000000)
val LightOnBackground = Color(0xFF000000)
val LightOnSurface = Color(0xFF000000)
val LightOnError = Color(0xFFFFFFFF)

val GreenBackground = Color(0xFF10B981)
val OrangeBackground = Color(0xFFF59E0B)
val RedBackground = Color(0xFFDC2626)
val GreenBox = Color(0xffeaf3e8)
val OrangeBox = Color(0xfffbf2cf)
val RedBox = Color(0xfff8d2b3)

// Dark theme colors
val DarkPrimary = Color(0xff643c80)
val DarkPrimaryContainer = Color(0xff454745)
val DarkSecondary = Color(0xff675c57)
val DarkSecondaryContainer = Color(0xff232222)
val DarkBackground = Color(0xff290000)
val DarkSurface = Color(0xff3a2b02)
val DarkError = Color(0xFFCF6679)
val DarkOnPrimary = Color(0xFF000000)
val DarkOnSecondary = Color(0xFF000000)
val DarkOnBackground = Color(0xFFFFFFFF)
val DarkOnSurface = Color(0xFFFFFFFF)
val DarkOnError = Color(0xFF000000)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    primaryContainer = LightPrimaryContainer,
    secondary = LightSecondary,
    secondaryContainer = LightSecondaryContainer,
    background = LightBackground,
    surface = LightSurface,
    error = LightError,
    onPrimary = LightOnPrimary,
    onSecondary = LightOnSecondary,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onError = LightOnError
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    primaryContainer = DarkPrimaryContainer,
    secondary = DarkSecondary,
    secondaryContainer = DarkSecondaryContainer,
    background = DarkBackground,
    surface = DarkSurface,
    error = DarkError,
    onPrimary = DarkOnPrimary,
    onSecondary = DarkOnSecondary,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onError = DarkOnError,
)

@Composable
fun RoundTimerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (!darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
