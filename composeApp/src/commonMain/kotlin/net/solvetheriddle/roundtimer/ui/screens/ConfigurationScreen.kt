package net.solvetheriddle.roundtimer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import net.solvetheriddle.roundtimer.ui.components.SetAppropriateStatusBarColor
import net.solvetheriddle.roundtimer.ui.components.StyledCard
import net.solvetheriddle.roundtimer.ui.components.CategoryList

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
    games: List<Game>,
    selectedPhase: String,
    selectedPlayer: String,
    customPhases: List<String>,
    playerCategories: List<String>,
    onPhaseSelect: (String) -> Unit,
    onPlayerSelect: (String) -> Unit,
    onAddCustomPhase: (String) -> Unit,
    onRemoveCustomPhase: (String) -> Unit,
    onRenameCustomPhase: (String, String) -> Unit,
    onAddPlayerCategory: (String) -> Unit,
    onRemovePlayerCategory: (String) -> Unit,
    onRenamePlayerCategory: (String, String) -> Unit,
    playerSuggestions: List<String> = emptyList()
) {
    SetAppropriateStatusBarColor()
    val isLandscape = rememberIsLandscape()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Box(
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
            contentAlignment = Alignment.Center
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // Left side: Selection and Navigation
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 32.dp)
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        CategoryList(
                            selectedPhase = selectedPhase,
                            selectedPlayer = selectedPlayer,
                            customPhases = customPhases,
                            playerCategories = playerCategories,
                            onPhaseSelect = onPhaseSelect,
                            onPlayerSelect = onPlayerSelect,
                            onAddCustomPhase = onAddCustomPhase,
                            onRemoveCustomPhase = onRemoveCustomPhase,
                            onRenameCustomPhase = onRenameCustomPhase,
                            onAddPlayerCategory = onAddPlayerCategory,
                            onRemovePlayerCategory = onRemovePlayerCategory,
                            onRenamePlayerCategory = onRenamePlayerCategory,
                            playerSuggestions = playerSuggestions,
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GamesButton(onGamesClick)
                            GameInfo(games, activeGameId, Modifier.weight(1f))
                            HistoryButton(onHistoryClick)
                        }
                    }

                    // Right side: Time selection
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        val cardSize = if (maxWidth > maxHeight) maxHeight else maxWidth * 1.2f
                        StyledCard(
                            modifier = Modifier.size(cardSize),
                            verticalArrangement = Arrangement.Top,
                            content = {
                                ScrollableDial(
                                    currentSeconds = state.configuredSeconds,
                                    onValueChange = onTimeChanged,
                                    formatTime = formatTime,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .heightIn(min = 150.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = onStartTimer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(40.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                ) {
                                    Text(
                                        text = "START",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        )
                    }
                }
            } else {
                // Portrait logic
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val cardSize = if (maxWidth > maxHeight) maxHeight else maxWidth * 0.9f
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CategoryList(
                            selectedPhase = selectedPhase,
                            selectedPlayer = selectedPlayer,
                            customPhases = customPhases,
                            playerCategories = playerCategories,
                            onPhaseSelect = onPhaseSelect,
                            onPlayerSelect = onPlayerSelect,
                            onAddCustomPhase = onAddCustomPhase,
                            onRemoveCustomPhase = onRemoveCustomPhase,
                            onRenameCustomPhase = onRenameCustomPhase,
                            onAddPlayerCategory = onAddPlayerCategory,
                            onRemovePlayerCategory = onRemovePlayerCategory,
                            onRenamePlayerCategory = onRenamePlayerCategory,
                            playerSuggestions = playerSuggestions,
                            modifier = Modifier
                                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                                .width(cardSize) // Align width with card
                        )
                        
                        // Centered StyledCard with scrollable dial
                        StyledCard(
                            modifier = Modifier.size(cardSize),
                            verticalArrangement = Arrangement.Top,
                            content = {
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

                                Button(
                                    onClick = onStartTimer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(40.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                ) {
                                    Text(
                                        text = "START",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        )

                        Spacer(Modifier.defaultMinSize(minHeight = 56.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GamesButton(onGamesClick)
                            GameInfo(games, activeGameId, Modifier.weight(1f))
                            HistoryButton(onHistoryClick)
                        }
                    }
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
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Default.List,
            contentDescription = "Games"
        )
    }
}

@Composable
private fun HistoryButton(onHistoryClick: () -> Unit) {
    FloatingActionButton(
        modifier = Modifier.size(68.dp),
        onClick = onHistoryClick,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = "History"
        )
    }
}
