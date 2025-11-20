package net.solvetheriddle.roundtimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.solvetheriddle.roundtimer.model.Round
import net.solvetheriddle.roundtimer.model.TimerState
import net.solvetheriddle.roundtimer.ui.components.CategoryList
import net.solvetheriddle.roundtimer.ui.components.SetAppropriateStatusBarColor
import net.solvetheriddle.roundtimer.ui.utils.rememberIsLandscape
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: TimerState,
    onNavigateUp: () -> Unit,
    onDeleteRound: (String) -> Unit,
    onUndoDelete: () -> Unit,
    onResetHistory: (String) -> Unit,
    formatTime: (Int) -> String
) {
    val isLandscape = rememberIsLandscape()

    // Local state for category filter
    var selectedFilterCategory by remember { mutableStateOf("All") }

    // Collect all unique categories from rounds plus defaults
    val allCategories = remember(state.rounds) {
        val categories = mutableSetOf("All", "Preparation", "Everyone")
        state.rounds.forEach { categories.add(it.category) }
        state.customTypes.forEach { categories.add(it) }
        state.playerTypes.forEach { categories.add(it) }
        categories.toList()
    }

    val filteredRounds = remember(state.rounds, selectedFilterCategory) {
        if (selectedFilterCategory == "All") {
            state.rounds
        } else {
            state.rounds.filter { it.category == selectedFilterCategory }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) } // Added for reset dialog

    SetAppropriateStatusBarColor()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.activeGameId != null) {
                        IconButton(onClick = { showResetDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Reset History")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        val lazyListState = rememberLazyListState()

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background,
                        )
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            if (state.rounds.isEmpty()) {
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
            } else {
                if (isLandscape) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left side: Stats + Filter
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            CategoryFilter(
                                categories = allCategories,
                                selectedCategory = selectedFilterCategory,
                                onCategorySelected = { selectedFilterCategory = it }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GameStats(filteredRounds, formatTime) // Use filteredRounds for stats
                        }

                        // Right side: Round List
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = filteredRounds.reversed(),
                                key = { it.id }
                            ) { round ->
                                val typeRounds = state.rounds.filter { it.category == round.category }
                                val typeIndex = typeRounds.indexOf(round) + 1
                                SwipeableRoundItem(
                                    round = round,
                                    roundNumber = typeIndex, // Pass type index instead of global index
                                    onDelete = {
                                        onDeleteRound(round.id)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Round deleted",
                                                actionLabel = "Undo"
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                onUndoDelete()
                                            }
                                        }
                                    },
                                    formatTime = formatTime
                                )
                            }
                        }
                    }
                } else {
                    // Portrait
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            CategoryFilter(
                                categories = allCategories,
                                selectedCategory = selectedFilterCategory,
                                onCategorySelected = { selectedFilterCategory = it }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GameStats(filteredRounds, formatTime) // Use filteredRounds for stats
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = filteredRounds.reversed(),
                                key = { it.id }
                            ) { round ->
                                val typeRounds = state.rounds.filter { it.category == round.category }
                                val typeIndex = typeRounds.indexOf(round) + 1
                                SwipeableRoundItem(
                                    round = round,
                                    roundNumber = typeIndex,
                                    onDelete = {
                                        onDeleteRound(round.id)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Round deleted",
                                                actionLabel = "Undo"
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                onUndoDelete()
                                            }
                                        }
                                    },
                                    formatTime = formatTime
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset History") },
                text = { Text("Are you sure you want to delete all rounds for this game? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        state.activeGameId?.let { onResetHistory(it) }
                        showResetDialog = false
                    }) {
                        Text("Reset", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun GameStats(rounds: List<Round>, formatTime: (Int) -> String) {
    // Calculate statistics
    val totalRounds = rounds.size
    val totalTime = rounds.sumOf { it.duration }
    val averageTime = if (totalRounds > 0) totalTime / totalRounds else 0
    val shortestRound = if (totalRounds > 0) rounds.minOf { it.duration } else 0
    val longestRound = if (totalRounds > 0) rounds.maxOf { it.duration } else 0

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
                StatisticItem("Total rounds", totalRounds.toString())
                StatisticItem("Total time", formatTime(totalTime))
                StatisticItem("Fastest round", formatTime(shortestRound))
                StatisticItem("Slowest round", formatTime(longestRound))
                StatisticItem("Average time", formatTime(averageTime))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryFilter(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = category == selectedCategory,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
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
                            text = "DELETE",
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
            // Left side: Round Number (now Category Name) and Time
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${round.category} #$roundNumber", // Display category name and number
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = formatTime(round.duration), // Duration only
                    fontSize = 14.sp,
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
