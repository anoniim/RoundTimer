package net.solvetheriddle.roundtimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.solvetheriddle.roundtimer.model.Game
import net.solvetheriddle.roundtimer.model.TimerState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GamesScreen(
    state: TimerState,
    onNavigateUp: () -> Unit,
    onCreateNewGame: (String) -> Unit,
    onSetActiveGame: (String) -> Unit,
    onUpdateGameName: (String, String) -> Unit,
    onDeleteGame: (String) -> Unit,
    onGameSelected: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var gameToEdit by remember { mutableStateOf<Game?>(null) }
    var showNewGameDialog by remember { mutableStateOf(false) }

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
            }
        )
    }

    if (showNewGameDialog) {
        NewGameDialog(
            onDismiss = { showNewGameDialog = false },
            onStart = { name ->
                onCreateNewGame(name)
                showNewGameDialog = false
                onGameSelected()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Games") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { showNewGameDialog = true },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    "Start new game",
                    fontSize = 30.sp
                )
            }
        }
    ) { paddingValues ->
        if (state.games.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Your games will be shown here")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                items(state.games) { game ->
                    GameListItem(
                        game = game,
                        isActive = game.id == state.activeGameId,
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

@Composable
private fun NewGameDialog(onDismiss: () -> Unit, onStart: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start New Game") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Game Name (Optional)") }
            )
        },
        confirmButton = {
            Button(onClick = { onStart(name) }) {
                Text("Start")
            }
        }
    )
}

@Composable
private fun EditGameNameDialog(game: Game, onDismiss: () -> Unit, onSave: (String) -> Unit, onDelete: () -> Unit) {
    var name by remember { mutableStateOf(game.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Game Name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Game Name") }
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
                Row {
                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(name) }) {
                        Text("Save")
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameListItem(game: Game, isActive: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 90.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(
                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
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
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(text = game.date)
        }
    }
}
