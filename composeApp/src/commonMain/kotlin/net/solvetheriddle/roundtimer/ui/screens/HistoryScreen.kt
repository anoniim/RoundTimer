package net.solvetheriddle.roundtimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.solvetheriddle.roundtimer.model.Round
import net.solvetheriddle.roundtimer.model.TimerState
import net.solvetheriddle.roundtimer.ui.utils.rememberIsLandscape
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: TimerState,
    onNavigateUp: () -> Unit,
    onDeleteRound: (String) -> Unit,
    onResetHistory: (String) -> Unit,
    formatTime: (Int) -> String
) {
    val filteredRounds = state.rounds.filter { it.gameId == state.activeGameId }

    // Calculate statistics
    val totalRounds = filteredRounds.size
    val totalTime = filteredRounds.sumOf { it.duration }
    val averageTime = if (totalRounds > 0) totalTime / totalRounds else 0
    val shortestRound = if (totalRounds > 0) filteredRounds.minOf { it.duration } else 0
    val longestRound = if (totalRounds > 0) filteredRounds.maxOf { it.duration } else 0
    val isLandscape = rememberIsLandscape()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Round History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (filteredRounds.isNotEmpty() && state.activeGameId != null) {
                        TextButton(
                            onClick = { onResetHistory(state.activeGameId!!) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = "Reset",
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (isLandscape) {
                            Spacer(modifier = Modifier.width(82.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        val lazyListState = rememberLazyListState()

        Box(
            modifier = Modifier.fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background,
                        )
                    )
                )
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (filteredRounds.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No rounds recorded yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Complete your first round to see it here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else if (isLandscape) {
                // Landscape: Split view
                Row(
                    modifier = Modifier
                        .widthIn(max = 1200.dp)
                        .fillMaxHeight()
                        .padding(horizontal = 84.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left side: Statistics
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 400.dp)
                            .fillMaxHeight()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "Statistics",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StatisticItem("Total Rounds", totalRounds.toString())
                                    StatisticItem("Total Time", formatTime(totalTime))
                                    StatisticItem("Shortest Round", formatTime(shortestRound))
                                    StatisticItem("Longest Round", formatTime(longestRound))
                                    StatisticItem("Average Time", formatTime(averageTime))
                                }
                            }
                        }
                    }

                    // Right side: Recent Rounds
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 400.dp)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Rounds list
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    state = lazyListState,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = filteredRounds,
                                        key = { it.id }
                                    ) { round ->
                                        SwipeableRoundItem(
                                            round = round,
                                            roundNumber = state.rounds.indexOf(round) + 1,
                                            onDelete = { onDeleteRound(round.id) },
                                            formatTime = formatTime
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Portrait: Vertical layout
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = lazyListState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Statistics section
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                                ) {
                                    Text(
                                        text = "Statistics",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        StatisticItem("Total Rounds", totalRounds.toString())
                                        StatisticItem("Total Time", formatTime(totalTime))
                                        StatisticItem("Shortest Round", formatTime(shortestRound))
                                        StatisticItem("Longest Round", formatTime(longestRound))
                                        StatisticItem("Average Time", formatTime(averageTime))
                                    }
                                }
                            }
                        }


                        // Round items
                        items(
                            items = filteredRounds,
                            key = { it.id }
                        ) { round ->
                            SwipeableRoundItem(
                                round = round,
                                roundNumber = state.rounds.indexOf(round) + 1,
                                onDelete = { onDeleteRound(round.id) },
                                formatTime = formatTime
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRoundItem(
    round: Round,
    roundNumber: Int,
    onDelete: () -> Unit,
    formatTime: (Int) -> String
) {
    val density = LocalDensity.current
    val threshold = with(density) { 120.dp.toPx() }
    
    val swipeableState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> threshold },
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    // Use a clipped container to ensure rounded corners are respected
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        SwipeToDismissBox(
            state = swipeableState,
            backgroundContent = {
                // Delete background - now properly clipped by the parent Box
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        ) {
            RoundItem(
                round = round,
                roundNumber = roundNumber,
                formatTime = formatTime
            )
        }
    }
}

@Composable
private fun RoundItem(
    round: Round,
    roundNumber: Int,
    formatTime: (Int) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Round info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Round #$roundNumber",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = formatTimestamp(round.timestamp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Right side: Total Duration (including overtime)
            Text(
                text = formatTime(round.duration + round.overtime),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun formatTimestamp(timestamp: Long): String {
    // Convert Long timestamp to Instant and format using kotlinx.datetime
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
}
