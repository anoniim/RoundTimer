package net.solvetheriddle.roundtimer

import androidx.compose.animation.Crossfade
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import net.solvetheriddle.roundtimer.ui.screens.ActiveTimerScreen
import net.solvetheriddle.roundtimer.ui.screens.ConfigurationScreen
import net.solvetheriddle.roundtimer.ui.screens.HistoryScreen
import net.solvetheriddle.roundtimer.viewmodel.TimerViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val viewModel: TimerViewModel = viewModel { TimerViewModel() }
        val state by viewModel.state.collectAsState()
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Configuration) }

        Crossfade(targetState = currentScreen) { screen ->
            when (screen) {
            is Screen.Configuration -> {
                ConfigurationScreen(
                    state = state,
                    onTimeChanged = viewModel::updateConfiguredTime,
                    onStartTimer = {
                        viewModel.startTimer()
                        currentScreen = Screen.ActiveTimer
                    },
                    onHistoryClick = { currentScreen = Screen.History },
                    formatTime = viewModel::formatTime
                )
            }
            is Screen.ActiveTimer -> {
                ActiveTimerScreen(
                    state = state,
                    onStopTimer = {
                        viewModel.stopTimer()
                        currentScreen = Screen.Configuration
                    },
                    formatTime = viewModel::formatTime
                )
            }
            is Screen.History -> {
                HistoryScreen(
                    state = state,
                    onNavigateUp = { currentScreen = Screen.Configuration },
                    onDeleteRound = viewModel::deleteRound,
                    onResetHistory = viewModel::resetHistory,
                    formatTime = viewModel::formatTime
                )
            }
        }
        }

        // Handle timer state changes from outside the UI
        LaunchedEffect(state.isRunning) {
            if (state.isRunning && currentScreen !is Screen.ActiveTimer) {
                currentScreen = Screen.ActiveTimer
            } else if (!state.isRunning && currentScreen is Screen.ActiveTimer) {
                currentScreen = Screen.Configuration
            }
        }
    }
}

sealed class Screen {
    data object Configuration : Screen()
    data object ActiveTimer : Screen()
    data object History : Screen()
}
