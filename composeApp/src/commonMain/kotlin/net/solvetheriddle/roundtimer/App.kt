package net.solvetheriddle.roundtimer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import net.solvetheriddle.roundtimer.ui.screens.ActiveTimerScreen
import net.solvetheriddle.roundtimer.ui.screens.ConfigurationScreen
import net.solvetheriddle.roundtimer.ui.screens.GamesScreen
import net.solvetheriddle.roundtimer.ui.screens.HistoryScreen
import net.solvetheriddle.roundtimer.viewmodel.TimerViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val viewModel: TimerViewModel = viewModel { TimerViewModel() }
        val state by viewModel.state.collectAsState()
        var currentScreenRoute by rememberSaveable { mutableStateOf(Screen.Configuration.route) }

        BackHandler(enabled = currentScreenRoute != Screen.Configuration.route) {
            when (currentScreenRoute) {
                Screen.History.route, Screen.Games.route -> {
                    currentScreenRoute = Screen.Configuration.route
                }
                Screen.ActiveTimer.route -> {
                    viewModel.stopTimer()
                    currentScreenRoute = Screen.Configuration.route
                }
            }
        }

        Crossfade(targetState = currentScreenRoute) { route ->
            when (route) {
                Screen.Configuration.route -> {
                    ConfigurationScreen(
                        state = state,
                        onTimeChanged = viewModel::updateConfiguredTime,
                        onStartTimer = {
                            viewModel.startTimer()
                            currentScreenRoute = Screen.ActiveTimer.route
                        },
                        onHistoryClick = { currentScreenRoute = Screen.History.route },
                        onGamesClick = { currentScreenRoute = Screen.Games.route },
                        formatTime = viewModel::formatTime
                    )
                }
                Screen.ActiveTimer.route -> {
                    ActiveTimerScreen(
                        state = state,
                        onStopTimer = {
                            viewModel.stopTimer()
                            currentScreenRoute = Screen.Configuration.route
                        },
                        formatTime = viewModel::formatTime
                    )
                }
                Screen.History.route -> {
                    HistoryScreen(
                        state = state,
                        onNavigateUp = { currentScreenRoute = Screen.Configuration.route },
                        onDeleteRound = viewModel::deleteRound,
                        onResetHistory = viewModel::resetHistory,
                        formatTime = viewModel::formatTime
                    )
                }
                Screen.Games.route -> {
                    GamesScreen(
                        onNavigateUp = { currentScreenRoute = Screen.Configuration.route }
                    )
                }
            }
        }

        // Handle timer state changes from outside the UI
        LaunchedEffect(state.isRunning) {
            if (state.isRunning && currentScreenRoute != Screen.ActiveTimer.route) {
                currentScreenRoute = Screen.ActiveTimer.route
            } else if (!state.isRunning && currentScreenRoute == Screen.ActiveTimer.route) {
                currentScreenRoute = Screen.Configuration.route
            }
        }
    }
}

sealed class Screen(val route: String) {
    data object Configuration : Screen("configuration")
    data object ActiveTimer : Screen("active_timer")
    data object History : Screen("history")
    data object Games : Screen("games")
}
