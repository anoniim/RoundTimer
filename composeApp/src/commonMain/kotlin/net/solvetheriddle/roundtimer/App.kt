package net.solvetheriddle.roundtimer

import androidx.compose.animation.Crossfade
import androidx.compose.material3.MaterialTheme
import net.solvetheriddle.roundtimer.ui.theme.RoundTimerTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import net.solvetheriddle.roundtimer.platform.isButtonNavigationEnabled
import net.solvetheriddle.roundtimer.ui.screens.ActiveTimerScreen
import net.solvetheriddle.roundtimer.ui.screens.ConfigurationScreen
import net.solvetheriddle.roundtimer.ui.screens.GamesScreen
import net.solvetheriddle.roundtimer.ui.screens.HistoryScreen
import net.solvetheriddle.roundtimer.ui.screens.SettingsScreen
import net.solvetheriddle.roundtimer.viewmodel.TimerViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    RoundTimerTheme {
        val viewModel: TimerViewModel = viewModel { TimerViewModel() }
        val state by viewModel.state.collectAsState()
        var currentScreenRoute by rememberSaveable { mutableStateOf(Screen.Configuration.route) }

        BackPressHandler(enabled = currentScreenRoute != Screen.Configuration.route) {
            when (currentScreenRoute) {
                Screen.History.route, Screen.Games.route -> {
                    currentScreenRoute = Screen.Configuration.route
                }
                Screen.Settings.route -> {
                    currentScreenRoute = Screen.Games.route
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
                        formatTime = viewModel::formatTime,
                        activeGameId = state.activeGameId,
                        games = state.games,
                        selectedCategory = state.selectedType,
                        customCategories = state.customTypes,
                        playerCategories = state.playerTypes,
                        onCategorySelect = viewModel::selectCategory,
                        onAddCustomCategory = viewModel::addCustomCategory,
                        onRemoveCustomCategory = viewModel::removeCustomCategory,
                        onRenameCustomCategory = viewModel::renameCustomCategory,
                        onAddPlayerCategory = viewModel::addPlayerCategory,
                        onRemovePlayerCategory = viewModel::removePlayerCategory,
                        onRenamePlayerCategory = viewModel::renamePlayerCategory
                    )
                }
                Screen.ActiveTimer.route -> {
                    val isOldNavigationEnabled = isButtonNavigationEnabled()
                    ActiveTimerScreen(
                        state = state,
                        onStopTimer = {
                            viewModel.stopTimer()
                            currentScreenRoute = Screen.Configuration.route
                        },
                        fastForward = { viewModel.fastForward(10) },
                        formatTime = viewModel::formatTime,
                        isOldNavigationEnabled = isOldNavigationEnabled
                    )
                }
                Screen.History.route -> {
                    HistoryScreen(
                        state = state,
                        onNavigateUp = { currentScreenRoute = Screen.Configuration.route },
                        onDeleteRound = viewModel::deleteRound,
                        onUndoDelete = viewModel::undoDeleteRound,
                        onResetHistory = viewModel::resetHistoryForGame,
                        onUpdateRound = viewModel::updateRound,
                        formatTime = viewModel::formatTime
                    )
                }
                Screen.Games.route -> {
                    GamesScreen(
                        state = state,
                        onNavigateUp = { currentScreenRoute = Screen.Configuration.route },
                        onNavigateToSettings = { currentScreenRoute = Screen.Settings.route },
                        onCreateNewGame = { name ->
                            viewModel.createNewGame(name)
                        },
                        onSetActiveGame = viewModel::setActiveGame,
                        onUpdateGameName = viewModel::updateGameName,
                        onDeleteGame = viewModel::deleteGame,
                        onUndoDelete = viewModel::undoDeleteGame,
                        onGameSelected = { currentScreenRoute = Screen.Configuration.route },
                        formatTime = viewModel::formatTime
                    )
                }
                Screen.Settings.route -> {
                    SettingsScreen(
                        settingsState = state.settings,
                        onNavigateUp = { currentScreenRoute = Screen.Games.route },
                        onSettingChanged = viewModel::updateSetting
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
    data object Settings : Screen("settings")
}
