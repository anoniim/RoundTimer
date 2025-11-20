package net.solvetheriddle.roundtimer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CategoryList(
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
    playerSuggestions: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp) // Unified spacing between sections
    ) {
        // Phase Section
        PhaseSection(
            fixedPhase = "Setup",
            customPhases = customPhases,
            selectedPhase = selectedPhase,
            onPhaseSelect = onPhaseSelect,
            onAddPhase = onAddCustomPhase,
            onRemovePhase = onRemoveCustomPhase,
            onRenamePhase = onRenameCustomPhase
        )

        // Player Section
        PlayerSection(
            fixedPlayer = "Everyone",
            players = playerCategories,
            selectedPlayer = selectedPlayer,
            onPlayerSelect = onPlayerSelect,
            onAddPlayer = onAddPlayerCategory,
            onRemovePlayer = onRemovePlayerCategory,
            onRenamePlayer = onRenamePlayerCategory,
            suggestions = playerSuggestions
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhaseSection(
    fixedPhase: String,
    customPhases: List<String>,
    selectedPhase: String,
    onPhaseSelect: (String) -> Unit,
    onAddPhase: (String) -> Unit,
    onRemovePhase: (String) -> Unit,
    onRenamePhase: (String, String) -> Unit
) {
    val allPhases = listOf(fixedPhase) + customPhases

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), // Center align
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        allPhases.forEach { phase ->
            CategoryChip(
                label = phase,
                isSelected = phase == selectedPhase,
                onClick = { onPhaseSelect(phase) },
                onLongClick = { },
                isEditable = phase != fixedPhase,
                onRemove = { onRemovePhase(phase) },
                onRename = { newName -> onRenamePhase(phase, newName) },
                editDialogTitle = "Edit phase",
                editDialogLabel = "Phase name"
            )
        }

        AddCategoryButton(
            onAdd = onAddPhase,
            dialogTitle = "Add phase",
            dialogLabel = "Phase name"
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerSection(
    fixedPlayer: String,
    players: List<String>,
    selectedPlayer: String,
    onPlayerSelect: (String) -> Unit,
    onAddPlayer: (String) -> Unit,
    onRemovePlayer: (String) -> Unit,
    onRenamePlayer: (String, String) -> Unit,
    suggestions: List<String>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // "Everyone" on its own row, full width (but visually just a wide chip or centered)
        // User asked for "Everyone" much wider and be in its row on its own.
        CategoryChip(
            label = fixedPlayer,
            isSelected = fixedPlayer == selectedPlayer,
            onClick = { onPlayerSelect(fixedPlayer) },
            onLongClick = { },
            isEditable = false,
            onRemove = { },
            onRename = { },
            editDialogTitle = "",
            editDialogLabel = "",
            modifier = Modifier.fillMaxWidth(0.8f) // Make it wider
        )

        // Other players + Add button
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), // Center align
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            players.forEach { player ->
                CategoryChip(
                    label = player,
                    isSelected = player == selectedPlayer,
                    onClick = { onPlayerSelect(player) },
                    onLongClick = { },
                    isEditable = true,
                    onRemove = { onRemovePlayer(player) },
                    onRename = { newName -> onRenamePlayer(player, newName) },
                    editDialogTitle = "Edit player",
                    editDialogLabel = "Player name"
                )
            }

            AddCategoryButton(
                onAdd = onAddPlayer,
                dialogTitle = "Add player",
                dialogLabel = "Player name",
                suggestions = suggestions,
                existingItems = players
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isEditable: Boolean,
    onRemove: () -> Unit,
    onRename: (String) -> Unit,
    editDialogTitle: String,
    editDialogLabel: String,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    
    if (showDialog) {
        EditCategoryDialog(
            initialName = label,
            onDismiss = { showDialog = false },
            onConfirm = { newName ->
                onRename(newName)
                showDialog = false
            },
            onDelete = {
                onRemove()
                showDialog = false
            },
            title = editDialogTitle,
            label = editDialogLabel
        )
    }

    Surface(
        modifier = modifier
            .height(40.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (isEditable) {
                        showDialog = true
                    }
                }
            ),
        shape = RoundedCornerShape(50),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(50)) // Clip content to shape to fix ripple clipping
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        if (isEditable) {
                            showDialog = true
                        }
                    }
                )
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AddCategoryButton(
    onAdd: (String) -> Unit,
    dialogTitle: String,
    dialogLabel: String,
    suggestions: List<String> = emptyList(),
    existingItems: List<String> = emptyList()
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AddCategoryDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name ->
                onAdd(name)
                showDialog = false
            },
            title = dialogTitle,
            label = dialogLabel,
            suggestions = suggestions,
            existingItems = existingItems
        )
    }

    Surface(
        modifier = Modifier
            .size(40.dp)
            .combinedClickable(
                onClick = { showDialog = true }
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Round Type",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String,
    label: String,
    suggestions: List<String> = emptyList(),
    existingItems: List<String> = emptyList()
) {
    var textState by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    var expanded by remember { mutableStateOf(false) }
    
    val filteredSuggestions = remember(textState.text, suggestions, existingItems) {
        val currentInput = textState.text.trim()
        // Filter out items that are already added
        val availableSuggestions = suggestions.filter { suggestion ->
            !existingItems.any { it.equals(suggestion, ignoreCase = true) }
        }
        
        if (currentInput.isBlank()) availableSuggestions
        else availableSuggestions.filter { 
            it.contains(currentInput, ignoreCase = true) && !it.equals(currentInput, ignoreCase = true) 
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Box {
                    OutlinedTextField(
                        value = textState,
                        onValueChange = { 
                            textState = it
                            expanded = true
                        },
                        label = { Text(label) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
                        ),
                        modifier = Modifier.focusRequester(focusRequester).fillMaxWidth()
                    )
                    
                    // Auto-expand dropdown when there are suggestions
                    LaunchedEffect(filteredSuggestions) {
                        if (filteredSuggestions.isNotEmpty()) {
                            expanded = true
                        }
                    }
                    
                    DropdownMenu(
                        expanded = expanded && filteredSuggestions.isNotEmpty(),
                        onDismissRequest = { expanded = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = false)
                    ) {
                        filteredSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    textState = androidx.compose.ui.text.input.TextFieldValue(
                                        text = suggestion,
                                        selection = androidx.compose.ui.text.TextRange(suggestion.length)
                                    )
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (textState.text.isNotBlank()) {
                        onConfirm(textState.text.trim())
                    }
                }
            ) {
                Text("Add")
            }
        }
    )
}

@Composable
private fun EditCategoryDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: () -> Unit,
    title: String,
    label: String
) {
    var text by remember { mutableStateOf(initialName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(label) },
                    singleLine = true
                )
            }
        },

        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete type")
                }
                TextButton(
                    onClick = { 
                        if (text.isNotBlank()) {
                            onConfirm(text.trim())
                        }
                    }
                ) {
                    Text("Rename")
                }
            }
        }
    )
}
