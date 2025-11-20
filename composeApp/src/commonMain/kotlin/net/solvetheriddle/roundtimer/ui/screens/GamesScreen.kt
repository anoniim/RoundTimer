package net.solvetheriddle.roundtimer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.solvetheriddle.roundtimer.model.Game
import net.solvetheriddle.roundtimer.model.TimerState
import net.solvetheriddle.roundtimer.ui.components.SetAppropriateStatusBarColor
import net.solvetheriddle.roundtimer.ui.utils.rememberIsLandscape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GamesScreen(
    state: TimerState,
    onNavigateUp: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onCreateNewGame: (String) -> Unit,
    onSetActiveGame: (String) -> Unit,
    onUpdateGameName: (String, String) -> Unit,
    onDeleteGame: (String) -> Unit,
    onUndoDelete: () -> Unit,
    onGameSelected: () -> Unit,
    formatTime: (Int) -> String
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var gameToEdit by remember { mutableStateOf<Game?>(null) }
    var showNewGameDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    SetAppropriateStatusBarColor()

    if (showEditDialog && gameToEdit != null) {
        EditGameNameDialog(
            game = gameToEdit!!,
            onDismiss = { showEditDialog = false },
            onSave = {
                onUpdateGameName(gameToEdit!!.id, it)
                showEditDialog = false
            },
            onDelete = {
                onDeleteGame(gameToEdit!!.id)
                showEditDialog = false
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Game deleted",
                        actionLabel = "Undo"
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onUndoDelete()
                    }
                }
            }
        )
    }

    if (showNewGameDialog) {
        NewGameDialog(
            existingGameNames = state.games.map { it.name }.filter { it.isNotEmpty() }.distinct(),
            onDismiss = { showNewGameDialog = false },
            onStart = { name ->
                onCreateNewGame(name)
                showNewGameDialog = false
                onGameSelected()
            }
        )
    }

    val isLandscape = rememberIsLandscape()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Games") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = if (isLandscape) Modifier.padding(end = 80.dp) else Modifier
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = if (!isLandscape) {
            {
                Button(
                    onClick = { showNewGameDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                        .padding(bottom = 32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        "START NEW GAME",
                        fontSize = 30.sp,
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        } else {
            { Spacer(Modifier.height(0.dp)) }
        }
    ) { paddingValues ->
        if (state.games.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background,
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Your games will be shown here", color = MaterialTheme.colorScheme.onBackground)
            }
        } else {
            Box(
                Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background,
                            )
                        )
                    )
                    .padding(paddingValues)
            ) {

                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(start = 96.dp)
                        ) {
                            items(state.games) { game ->
                                val gameRounds = state.rounds.filter { it.gameId == game.id }
                                val totalRounds = gameRounds.size
                                val totalTime = gameRounds.sumOf { it.duration }
                                val averageTime = if (totalRounds > 0) totalTime / totalRounds else 0
                                GameListItem(
                                    game = game,
                                    isActive = game.id == state.activeGameId,
                                    totalRounds = totalRounds,
                                    averageTime = averageTime,
                                    formatTime = formatTime,
                                    onClick = {
                                        onSetActiveGame(game.id)
                                        onGameSelected()
                                    },
                                    onLongClick = {
                                        gameToEdit = game
                                        showEditDialog = true
                                    }
                                )
                            }
                            item {
                                Spacer(Modifier.height(32.dp))
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 16.dp)
                                .padding(horizontal = 48.dp)
                                .padding(end = 48.dp, bottom = 32.dp)
                                .width(150.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { showNewGameDialog = true },
                                modifier = Modifier.fillMaxSize(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = "START\nNEW\nGAME",
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = 1.sp,
                                    lineHeight = 40.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(state.games) { game ->
                            val gameRounds = state.rounds.filter { it.gameId == game.id }
                            val totalRounds = gameRounds.size
                            val totalTime = gameRounds.sumOf { it.duration }
                            val averageTime = if (totalRounds > 0) totalTime / totalRounds else 0
                            GameListItem(
                                game = game,
                                isActive = game.id == state.activeGameId,
                                totalRounds = totalRounds,
                                averageTime = averageTime,
                                formatTime = formatTime,
                                onClick = {
                                    onSetActiveGame(game.id)
                                    onGameSelected()
                                },
                                onLongClick = {
                                    gameToEdit = game
                                    showEditDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewGameDialog(
    existingGameNames: List<String>,
    onDismiss: () -> Unit, 
    onStart: (String) -> Unit
) {
    var nameState by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    var expanded by remember { mutableStateOf(false) }
    
    val filteredOptions = remember(nameState.text, existingGameNames) {
        if (nameState.text.isBlank()) existingGameNames
        else existingGameNames.filter { it.contains(nameState.text, ignoreCase = true) && !it.equals(nameState.text, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start new game", color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column {
                Box {
                    OutlinedTextField(
                        value = nameState,
                        onValueChange = { 
                            nameState = it 
                            expanded = true
                        },
                        label = { Text("Game name (optional)") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onStart(nameState.text) }),
                        modifier = Modifier.focusRequester(focusRequester).fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.onBackground,
                        )
                    )
                    
                    DropdownMenu(
                        expanded = expanded && filteredOptions.isNotEmpty(),
                        onDismissRequest = { expanded = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = false)
                    ) {
                        filteredOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    nameState = androidx.compose.ui.text.input.TextFieldValue(
                                        text = option,
                                        selection = androidx.compose.ui.text.TextRange(option.length)
                                    )
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Text(
                    text = "Select a previous game to reuse its round types",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        },
        confirmButton = {
            Button(onClick = { onStart(nameState.text) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("START", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun EditGameNameDialog(game: Game, onDismiss: () -> Unit, onSave: (String) -> Unit, onDelete: () -> Unit) {
    var name by remember { mutableStateOf(game.name) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit game", color = MaterialTheme.colorScheme.onBackground) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Game name") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave(name) }),
                modifier = Modifier.focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
                Button(
                    onClick = { onSave(name) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameListItem(
    game: Game,
    isActive: Boolean,
    totalRounds: Int,
    averageTime: Int,
    formatTime: (Int) -> String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 90.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(
                color = if (isActive) MaterialTheme.colorScheme.secondary else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            if (game.name.isNotEmpty()) {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Text(
                text = game.date,
                style = if (game.name.isEmpty()) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "$totalRounds rounds", color = MaterialTheme.colorScheme.onBackground)
            Text(text = "Avg ${formatTime(averageTime)}", color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
