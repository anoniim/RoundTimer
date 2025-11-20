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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.solvetheriddle.roundtimer.model.Round
import net.solvetheriddle.roundtimer.model.TimerState
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
    onUpdateRound: (String, Int, Int, String, String) -> Unit,
    formatTime: (Int) -> String
) {
    val isLandscape = rememberIsLandscape()

    // Local state for filters
    var selectedFilterPhase by remember { mutableStateOf("All") }
    var selectedFilterPlayer by remember { mutableStateOf("All") }

    // Collect unique phases and players from rounds in the current game
    val (allPhases, allPlayers) = remember(state.rounds, state.activeGameId) {
        val phases = mutableSetOf("All")
        val players = mutableSetOf("All")
        
        val gameRounds = if (state.activeGameId != null) {
            state.rounds.filter { it.gameId == state.activeGameId }
        } else {
            state.rounds
        }
        
        gameRounds.forEach { 
            phases.add(it.phase)
            players.add(it.player)
        }
        
        Pair(phases.toList().sorted(), players.toList().sorted())
    }

    // Filtered rounds logic
    val filteredRounds = remember(state.rounds, state.activeGameId, selectedFilterPhase, selectedFilterPlayer) {
        var rounds = if (state.activeGameId != null) {
            state.rounds.filter { it.gameId == state.activeGameId }
        } else {
            state.rounds
        }

        if (selectedFilterPhase != "All") {
            rounds = rounds.filter { it.phase == selectedFilterPhase }
        }
        
        if (selectedFilterPlayer != "All") {
            rounds = rounds.filter { it.player == selectedFilterPlayer }
        }
        
        rounds
    }

    // Available players for the selected phase (for dependent filtering)
    val availablePlayersForFilter = remember(state.rounds, state.activeGameId, selectedFilterPhase) {
        if (selectedFilterPhase == "All") {
            allPlayers
        } else {
            val players = mutableSetOf("All")
            val gameRounds = if (state.activeGameId != null) {
                state.rounds.filter { it.gameId == state.activeGameId }
            } else {
                state.rounds
            }
            gameRounds.filter { it.phase == selectedFilterPhase }.forEach { players.add(it.player) }
            players.toList().sorted()
        }
    }

    // Calculate available options for editing (include configured types + existing in rounds)
    val availableEditPhases = remember(state.rounds, state.activeGameId, state.games, state.customTypes) {
        val phases = mutableSetOf("Setup")
        val activeGame = state.games.find { it.id == state.activeGameId }
        if (activeGame != null) {
            phases.addAll(activeGame.customTypes)
        } else {
            phases.addAll(state.customTypes)
        }
        // Add from existing rounds
        val gameRounds = if (state.activeGameId != null) state.rounds.filter { it.gameId == state.activeGameId } else state.rounds
        gameRounds.forEach { phases.add(it.phase) }
        phases.toList().sorted()
    }

    val availableEditPlayers = remember(state.rounds, state.activeGameId, state.games, state.playerTypes) {
        val players = mutableSetOf("Everyone")
        val activeGame = state.games.find { it.id == state.activeGameId }
        if (activeGame != null) {
            players.addAll(activeGame.playerTypes)
        } else {
            players.addAll(state.playerTypes)
        }
        // Add from existing rounds
        val gameRounds = if (state.activeGameId != null) state.rounds.filter { it.gameId == state.activeGameId } else state.rounds
        gameRounds.forEach { players.add(it.player) }
        players.toList().sorted()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }
    var editingRound by remember { mutableStateOf<Round?>(null) }

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
                            FiltersSection(
                                phases = allPhases,
                                selectedPhase = selectedFilterPhase,
                                onPhaseSelected = { 
                                    selectedFilterPhase = it 
                                    selectedFilterPlayer = "All" // Reset player filter when phase changes
                                },
                                players = availablePlayersForFilter,
                                selectedPlayer = selectedFilterPlayer,
                                onPlayerSelected = { selectedFilterPlayer = it }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GameStats(filteredRounds, formatTime)
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
                                // Calculate index within the current filter context or global context?
                                // Usually round number is per game.
                                // Let's show the index relative to the game rounds of the same type (Phase+Player)
                                val typeRounds = state.rounds.filter { 
                                    it.gameId == state.activeGameId && 
                                    it.phase == round.phase && 
                                    it.player == round.player 
                                }
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
                                    onLongClick = { editingRound = round },
                                    formatTime = formatTime
                                )
                            }
                        }
                    }
                } else {
                    // Portrait
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            FiltersSection(
                                phases = allPhases,
                                selectedPhase = selectedFilterPhase,
                                onPhaseSelected = { 
                                    selectedFilterPhase = it 
                                    selectedFilterPlayer = "All" // Reset player filter when phase changes
                                },
                                players = availablePlayersForFilter,
                                selectedPlayer = selectedFilterPlayer,
                                onPlayerSelected = { selectedFilterPlayer = it }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GameStats(filteredRounds, formatTime)
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
                                val typeRounds = state.rounds.filter { 
                                    it.gameId == state.activeGameId && 
                                    it.phase == round.phase && 
                                    it.player == round.player 
                                }
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
                                    onLongClick = { editingRound = round },
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
        
        editingRound?.let { round ->
            EditRoundDialog(
                round = round,
                availablePhases = availableEditPhases,
                availablePlayers = availableEditPlayers,
                formatTime = formatTime,
                onDismiss = { editingRound = null },
                onSave = { newDuration, newOvertime, newPhase, newPlayer ->
                    onUpdateRound(round.id, newDuration, newOvertime, newPhase, newPlayer)
                    editingRound = null
                },
                onDelete = {
                    onDeleteRound(round.id)
                    editingRound = null
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Round deleted",
                            actionLabel = "Undo"
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            onUndoDelete()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun FiltersSection(
    phases: List<String>,
    selectedPhase: String,
    onPhaseSelected: (String) -> Unit,
    players: List<String>,
    selectedPlayer: String,
    onPlayerSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Phase Filter
        CategoryFilter(
            categories = phases,
            selectedCategory = selectedPhase,
            onCategorySelected = onPhaseSelected,
            label = "Phase"
        )
        
        // Player Filter
        CategoryFilter(
            categories = players,
            selectedCategory = selectedPlayer,
            onCategorySelected = onPlayerSelected,
            label = "Player"
        )
    }
}

@Composable
private fun GameStats(rounds: List<Round>, formatTime: (Int) -> String) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val totalRounds = rounds.size
    val totalTime = rounds.sumOf { it.duration }
    val averageTime = if (totalRounds > 0) totalTime / totalRounds else 0
    val shortestRound = if (totalRounds > 0) rounds.minOf { it.duration } else 0
    val longestRound = if (totalRounds > 0) rounds.maxOf { it.duration } else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryFilter(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    label: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
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
    onLongClick: () -> Unit,
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        SwipeToDismissBox(
            state = swipeableState,
            backgroundContent = {
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
                onLongClick = onLongClick,
                formatTime = formatTime
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoundItem(
    round: Round,
    roundNumber: Int,
    onLongClick: () -> Unit,
    formatTime: (Int) -> String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${round.phase} - ${round.player} #$roundNumber",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Calculate start time
                val startTimeMillis = round.timestamp - (round.duration + round.overtime) * 1000L
                Text(
                    text = formatTimestamp(startTimeMillis),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatTime(round.duration + round.overtime),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun RepeatingIconButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    enabled: Boolean = true,
    initialDelayMillis: Long = 500,
    intervalMillis: Long = 200
) {
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            onClick()
            delay(initialDelayMillis)
            while (isPressed) {
                onClick()
                delay(intervalMillis)
            }
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) {
            icon()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRoundDialog(
    round: Round,
    availablePhases: List<String>,
    availablePlayers: List<String>,
    formatTime: (Int) -> String,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String, String) -> Unit,
    onDelete: () -> Unit
) {
    var timeSeconds by remember { mutableStateOf(round.duration) }
    var overtimeSeconds by remember { mutableStateOf(round.overtime) }
    var selectedPhase by remember { mutableStateOf(round.phase) }
    var selectedPlayer by remember { mutableStateOf(round.player) }
    
    var phaseExpanded by remember { mutableStateOf(false) }
    var playerExpanded by remember { mutableStateOf(false) }

    val totalSeconds = timeSeconds + overtimeSeconds
    val hadInitialOvertime = remember { round.overtime > 0 }

    val handleIncrement = {
        timeSeconds += 5
    }

    val handleDecrement = {
        if (overtimeSeconds > 0) {
            overtimeSeconds = maxOf(0, overtimeSeconds - 5)
        } else {
            timeSeconds = maxOf(0, timeSeconds - 5)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit round") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Phase selector
                Column {
                    Text(
                        text = "Phase",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExposedDropdownMenuBox(
                        expanded = phaseExpanded,
                        onExpandedChange = { phaseExpanded = !phaseExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPhase,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = phaseExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = phaseExpanded,
                            onDismissRequest = { phaseExpanded = false }
                        ) {
                            availablePhases.forEach { phase ->
                                DropdownMenuItem(
                                    text = { Text(phase) },
                                    onClick = {
                                        selectedPhase = phase
                                        phaseExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Player selector
                Column {
                    Text(
                        text = "Player",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExposedDropdownMenuBox(
                        expanded = playerExpanded,
                        onExpandedChange = { playerExpanded = !playerExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPlayer,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = playerExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = playerExpanded,
                            onDismissRequest = { playerExpanded = false }
                        ) {
                            availablePlayers.forEach { player ->
                                DropdownMenuItem(
                                    text = { Text(player) },
                                    onClick = {
                                        selectedPlayer = player
                                        playerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Total Duration editing
                Column {
                    Text(
                        text = "Total Duration",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RepeatingIconButton(
                            onClick = handleDecrement,
                            icon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease") }
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formatTime(totalSeconds),
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (hadInitialOvertime && overtimeSeconds > 0) {
                                Text(
                                    text = "${formatTime(timeSeconds)} + ${formatTime(overtimeSeconds)} OT",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                        RepeatingIconButton(
                            onClick = handleIncrement,
                            icon = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete round")
                }
                Button(onClick = { onSave(timeSeconds, overtimeSeconds, selectedPhase, selectedPlayer) }) {
                    Text("Save")
                }
            }
        }
    )
}

@OptIn(ExperimentalTime::class)
private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
}
