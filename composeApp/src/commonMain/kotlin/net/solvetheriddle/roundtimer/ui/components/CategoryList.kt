package net.solvetheriddle.roundtimer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CategoryList(
    selectedCategory: String,
    customCategories: List<String>,
    playerCategories: List<String>,
    onCategorySelect: (String) -> Unit,
    onAddCustomCategory: (String) -> Unit,
    onRemoveCustomCategory: (String) -> Unit,
    onRenameCustomCategory: (String, String) -> Unit,
    onAddPlayerCategory: (String) -> Unit,
    onRemovePlayerCategory: (String) -> Unit,
    onRenamePlayerCategory: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Preparation + Custom Categories
        CategoryRow(
            fixedCategory = "Preparation",
            dynamicCategories = customCategories,
            selectedCategory = selectedCategory,
            onCategorySelect = onCategorySelect,
            onAddCategory = onAddCustomCategory,
            onRemoveCategory = onRemoveCustomCategory,
            onRenameCategory = onRenameCustomCategory,
            maxItems = 4,
            addDialogTitle = "Add round type",
            addDialogLabel = "Round type",
            editDialogTitle = "Edit round type",
            editDialogLabel = "Round type"
        )

        // Row 2: Everyone + Player Categories
        CategoryRow(
            fixedCategory = "Everyone",
            dynamicCategories = playerCategories,
            selectedCategory = selectedCategory,
            onCategorySelect = onCategorySelect,
            onAddCategory = onAddPlayerCategory,
            onRemoveCategory = onRemovePlayerCategory,
            onRenameCategory = onRenamePlayerCategory,
            maxItems = 4,
            addDialogTitle = "Add player",
            addDialogLabel = "Player name",
            editDialogTitle = "Edit player",
            editDialogLabel = "Player name"
        )
    }
}

@Composable
private fun CategoryRow(
    fixedCategory: String,
    dynamicCategories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onRemoveCategory: (String) -> Unit,
    onRenameCategory: (String, String) -> Unit,

    maxItems: Int,
    addDialogTitle: String,
    addDialogLabel: String,
    editDialogTitle: String,
    editDialogLabel: String
) {
    // We need to handle wrapping if items exceed maxItems per row
    // But the requirement says "When the maximum number in a row is reached, a new row is added below."
    // So we'll just use a FlowRow-like logic or simply multiple rows if needed.
    // For simplicity, let's chunk the dynamic categories.
    
    val allItems = listOf(fixedCategory) + dynamicCategories
    val rows = allItems.chunked(maxItems + 1) // +1 because we have the fixed category in the first chunk effectively, wait.
    // Actually, "Preparation" is 1 item. Custom categories are added.
    // "1st row: Preparation, custom (none initially, any number can be added by a '+' button)"
    // "The user adds custom categories and players by clicking a '+' button (up to 4 in one row). Clicking + button adds a new label to the row. When the maximum number in a row is reached, a new row is added below."
    
    // Let's treat the fixed category as just the first item of the first row.
    // And we append the "+" button at the end of the last row if there's space, or new row.
    
    // Let's construct the list of items to display including the add button logic visually
    
    val itemsToDisplay = ArrayList<String>()
    itemsToDisplay.add(fixedCategory)
    itemsToDisplay.addAll(dynamicCategories)
    
    // Chunking
    // We want max 4 items per row? "up to 4 in one row"
    // Let's assume 4 items MAX per row including the fixed one for the first row.
    
    val chunks = itemsToDisplay.chunked(4)
    
    chunks.forEachIndexed { index, chunk ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            chunk.forEach { category ->
                CategoryChip(
                    label = category,
                    isSelected = category == selectedCategory,
                    onClick = { onCategorySelect(category) },
                    onLongClick = {
                        if (category != fixedCategory) {
                            // Show edit/delete dialog
                            // We need to handle this via a callback that opens a dialog
                            // For now, let's just pass the category to a handler that might show a dialog
                            // But wait, we need to know if it's a rename or delete.
                            // We can pass a "onEdit" callback.
                        }
                    },
                    isEditable = category != fixedCategory,
                    onRemove = { onRemoveCategory(category) },
                    onRename = { newName -> onRenameCategory(category, newName) },
                    editDialogTitle = editDialogTitle,
                    editDialogLabel = editDialogLabel
                )
            }
            
            // Add button logic
            // If this is the last chunk and it has less than 4 items, we add the + button here.
            // OR if this is the last chunk and it has 4 items, we need a new row for the + button?
            // The requirement says "When the maximum number in a row is reached, a new row is added below."
            // This implies the + button might move to the next row.
            
            if (index == chunks.lastIndex) {
                if (chunk.size < 4) {
                    AddCategoryButton(
                        onAdd = onAddCategory,
                        dialogTitle = addDialogTitle,
                        dialogLabel = addDialogLabel
                    )
                }
            }
        }
        
        // If the last chunk was full (4 items), we need a new row just for the + button
        if (index == chunks.lastIndex && chunk.size == 4) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AddCategoryButton(
                    onAdd = onAddCategory,
                    dialogTitle = addDialogTitle,
                    dialogLabel = addDialogLabel
                )
            }
        }
    }
    
    // If there are no dynamic categories, we still have the fixed one.
    // The loop above handles it.
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
    editDialogLabel: String
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
        modifier = Modifier
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
            modifier = Modifier.padding(horizontal = 16.dp)
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
    dialogLabel: String
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
            label = dialogLabel
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
    label: String
) {
    var text by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (text.isNotBlank()) {
                        onConfirm(text)
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
            TextButton(
                onClick = { 
                    if (text.isNotBlank()) {
                        onConfirm(text)
                    }
                }
            ) {
                Text("Rename")
            }
        }
    )
}
