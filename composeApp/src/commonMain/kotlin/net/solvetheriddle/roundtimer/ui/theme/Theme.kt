package net.solvetheriddle.roundtimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light theme colors
val LightPrimary = Color(0xffd80073)
val LightPrimaryContainer = Color(0xfffbd4ea)
val LightSecondary = Color(0xffdacabb)
val LightTertiary = Color(0xffaa6786)
val LightSecondaryContainer = Color(0xfff8edde)
val LightBackground = Color(0xfff8d2b3)
val LightSurface = Color(0xfffff5d5)
val LightError = Color(0xff000000)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightOnSecondary = Color(0xFF000000)
val LightOnBackground = Color(0xff373737)
val LightOnSurface = Color(0xff494248)
val LightOnError = Color(0xFFFFFFFF)

val GreenBackground = Color(0xFF10B981)
val OrangeBackground = Color(0xFFF59E0B)
val RedBackground = Color(0xFFDC2626)
val GreenBox = Color(0xffeaf3e8)
val OrangeBox = Color(0xfffbf2cf)
val RedBox = Color(0xfff8d2b3)

// Dark theme colors
val DarkPrimary = Color(0xff870048)
val DarkPrimaryContainer = Color(0xff450026)
val DarkSecondary = Color(0xff675c57)
val DarkTertiary = Color(0xFF03DAC5)
val DarkSecondaryContainer = Color(0xff232222)
val DarkBackground = Color(0xff2e0404)
val DarkSurface = Color(0xff977200)
val DarkError = Color(0xffd3a3ae)
val DarkOnPrimary = Color(0xffa3a3a3)
val DarkOnSecondary = Color(0xFF000000)
val DarkOnBackground = Color(0xffc3c3c3)
val DarkOnSurface = Color(0xffbda8b6)
val DarkOnError = Color(0xFF000000)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    primaryContainer = LightPrimaryContainer,
    secondary = LightSecondary,
    secondaryContainer = LightSecondaryContainer,
    tertiary = LightTertiary,
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
    tertiary = DarkTertiary,
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
    val colors = if (darkTheme) {
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
