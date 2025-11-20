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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.graphics.Color
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
    onUpdateRound: (String, Int, Int, String) -> Unit,
    formatTime: (Int) -> String
) {
    val isLandscape = rememberIsLandscape()

    // Local state for category filter
    var selectedFilterCategory by remember { mutableStateOf("All") }

    // Collect all unique categories from rounds in the current game
    val allCategories = remember(state.rounds, state.activeGameId) {
        val categories = mutableSetOf<String>("All")
        val gameRounds = if (state.activeGameId != null) {
            state.rounds.filter { it.gameId == state.activeGameId }
        } else {
            state.rounds
        }
        gameRounds.forEach { categories.add(it.category) }
        categories.toList()
    }

    val filteredRounds = remember(state.rounds, state.activeGameId, selectedFilterCategory) {
        // First filter by active game
        val gameRounds = if (state.activeGameId != null) {
            state.rounds.filter { it.gameId == state.activeGameId }
        } else {
            state.rounds
        }
        
        // Then filter by category
        if (selectedFilterCategory == "All") {
            gameRounds
        } else {
            gameRounds.filter { it.category == selectedFilterCategory }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) } // Added for reset dialog
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
                                val typeRounds = state.rounds.filter { it.gameId == state.activeGameId && it.category == round.category }
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
                                val typeRounds = state.rounds.filter { it.gameId == state.activeGameId && it.category == round.category }
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
                availableCategories = allCategories.filter { it != "All" },
                formatTime = formatTime,
                onDismiss = { editingRound = null },
                onSave = { newDuration, newOvertime, newCategory ->
                    onUpdateRound(round.id, newDuration, newOvertime, newCategory)
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
private fun GameStats(rounds: List<Round>, formatTime: (Int) -> String) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Calculate statistics
    val totalRounds = rounds.size
    val totalTime = rounds.sumOf { it.duration }
    val averageTime = if (totalRounds > 0) totalTime / totalRounds else 0
    val shortestRound = if (totalRounds > 0) rounds.minOf { it.duration } else 0
    val longestRound = if (totalRounds > 0) rounds.maxOf { it.duration } else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(),
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
            if (isExpanded) {
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
            // First click happens immediately
            onClick()
            // Wait before starting continuous updates
            delay(initialDelayMillis)
            // Then repeat continuously
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
    availableCategories: List<String>,
    formatTime: (Int) -> String,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String) -> Unit,
    onDelete: () -> Unit
) {
    var timeSeconds by remember { mutableStateOf(round.duration) }
    var overtimeSeconds by remember { mutableStateOf(round.overtime) }
    var selectedCategory by remember { mutableStateOf(round.category) }
    var expanded by remember { mutableStateOf(false) }

    val totalSeconds = timeSeconds + overtimeSeconds
    val hadInitialOvertime = remember { round.overtime > 0 }

    // Handle increment: always add to duration
    val handleIncrement = {
        timeSeconds += 5
    }

    // Handle decrement: remove from overtime first, then from duration
    val handleDecrement = {
        if (overtimeSeconds > 0) {
            // If there's overtime, decrease it first
            overtimeSeconds = maxOf(0, overtimeSeconds - 5)
        } else {
            // Otherwise decrease duration (but not below 0)
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
                // Category/Type selector
                Column {
                    Text(
                        text = "Type",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        expanded = false
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
                Button(onClick = { onSave(timeSeconds, overtimeSeconds, selectedCategory) }) {
                    Text("Save")
                }
            }
        }
    )
}

@OptIn(ExperimentalTime::class)
private fun formatTimestamp(timestamp: Long): String {
    // Convert Long timestamp to Instant and format using kotlinx.datetime
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
}
