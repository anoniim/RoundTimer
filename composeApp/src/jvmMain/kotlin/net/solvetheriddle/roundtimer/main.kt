package net.solvetheriddle.roundtimer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.io.BufferedReader
import java.io.InputStreamReader

fun main() {
    manageDarkModeOnMac()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "RoundTimer",
        ) {
            App()
        }
    }
}

private fun manageDarkModeOnMac() {
    val isMac = System.getProperty("os.name").contains("Mac")
    var isDarkMode = false
    if (isMac) {
        try {
            val process = Runtime.getRuntime().exec("defaults read -g AppleInterfaceStyle")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            if (reader.readLine()?.contains("Dark") == true) {
                isDarkMode = true
            }
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (isDarkMode) {
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameVibrantDark")
    } else {
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameVibrantLight")
    }
}