package net.solvetheriddle.roundtimer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.solvetheriddle.roundtimer.model.Game
import net.solvetheriddle.roundtimer.model.TimerState
import net.solvetheriddle.roundtimer.ui.utils.rememberIsLandscape
import net.solvetheriddle.roundtimer.platform.getStatusBarManager
import net.solvetheriddle.roundtimer.ui.components.ScrollableDial
import net.solvetheriddle.roundtimer.ui.components.StyledCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConfigurationScreen(
    state: TimerState,
    onTimeChanged: (Int) -> Unit,
    onStartTimer: () -> Unit,
    onHistoryClick: () -> Unit,
    onGamesClick: () -> Unit,
    formatTime: (Int) -> String,
    activeGameId: String?,
    games: List<Game>
) {
    // Set status bar to dark content for light mode
    LaunchedEffect(Unit) {
        try {
            getStatusBarManager().setStatusBarStyle(isDarkContent = true)
        } catch (e: Exception) {
            // Handle platforms that don't support status bar styling
        }
    }
    val isLandscape = rememberIsLandscape()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GamesButton(onGamesClick)
                if (!isLandscape) {
                    GameInfo(games, activeGameId, Modifier.weight(1f))
                }
                HistoryButton(onHistoryClick)
            }
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            if (isLandscape) {
                GameInfo(games, activeGameId, Modifier.align(Alignment.TopStart).padding(32.dp).padding(top = 16.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background,
                            )
                        )
                    )
                    .padding(paddingValues),
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val cardSize = if (maxWidth > maxHeight) maxHeight else maxWidth * 0.9f
                    // Centered StyledCard with scrollable dial
                    StyledCard(
                        modifier = Modifier.size(cardSize),
                        verticalArrangement = Arrangement.Top,
                        content = {
                            // Scrollable dial for time selection
                            ScrollableDial(
                                currentSeconds = state.configuredSeconds,
                                onValueChange = onTimeChanged,
                                formatTime = formatTime,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .heightIn(min = 200.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Start button
                            Button(
                                onClick = onStartTimer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                            ) {
                                Text(
                                    text = "START",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    }
}

@Composable
private fun GameInfo(
    games: List<Game>,
    activeGameId: String?,
    modifier: Modifier = Modifier
) {
    val activeGame = games.find { it.id == activeGameId }
    if (activeGame != null) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (activeGame.name.isNotEmpty()) {
                Text(
                    text = activeGame.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = activeGame.date,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun GamesButton(onGamesClick: () -> Unit) {
    FloatingActionButton(
        modifier = Modifier.size(68.dp),
        onClick = onGamesClick,
        containerColor = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Icon(
            imageVector = Icons.Filled.List,
            contentDescription = "Games"
        )
    }
}

@Composable
private fun HistoryButton(onHistoryClick: () -> Unit) {
    FloatingActionButton(
        modifier = Modifier.size(68.dp),
        onClick = onHistoryClick,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = "History"
        )
    }
}
