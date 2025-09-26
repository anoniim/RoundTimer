package net.solvetheriddle.roundtimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.solvetheriddle.roundtimer.model.TimerState
import net.solvetheriddle.roundtimer.platform.getStatusBarManager
import net.solvetheriddle.roundtimer.ui.components.StyledCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    state: TimerState,
    onTimeChanged: (Int) -> Unit,
    onStartTimer: () -> Unit,
    onHistoryClick: () -> Unit,
    formatTime: (Int) -> String
) {
    // Set status bar to dark content for light mode
    LaunchedEffect(Unit) {
        try {
            getStatusBarManager().setStatusBarStyle(isDarkContent = true)
        } catch (e: Exception) {
            // Handle platforms that don't support status bar styling
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            HistoryButton(onHistoryClick)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8F9FA),
                            Color(0xFFE9ECEF)
                        )
                    )
                )
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .padding(bottom = 88.dp),
                contentAlignment = Alignment.Center
            ) {
                // Vertical slider positioned on the right side
                Slider(
                    value = state.configuredSeconds.toFloat(),
                    onValueChange = { onTimeChanged(it.toInt()) },
                    valueRange = 30f..600f, // 30 seconds to 10 minutes
                    steps = 56, // 600-30/10 - 1
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .graphicsLayer { // Using graphicsLayer for better performance with rotation
                            rotationZ = 270f // Rotate -90 degrees or 270 degrees
                            // Optional: Adjust transformOrigin if needed, though Center is often fine
                            // transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                        .layout { measurable, constraints ->
                            // This layout modifier swaps width and height for the rotated slider
                            // so it's measured correctly within the parent Box.
                            val placeable = measurable.measure(
                                Constraints(
                                    minWidth = constraints.minHeight,
                                    maxWidth = constraints.maxHeight,
                                    minHeight = constraints.minWidth,
                                    maxHeight = constraints.maxWidth,
                                )
                            )
                            layout(placeable.height, placeable.width) {
                                placeable.placeRelative(
                                    (placeable.height - placeable.width) / 2, // Center the slider vertically
                                    (placeable.width - placeable.height) / 2 // Center the slider horizontally
                                )
                            }
                        },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Centered StyledCard with timer configuration
                StyledCard(
                    modifier = Modifier
                        .clickable { onStartTimer() }
                ) {
                    // Large time display
                    Text(
                        text = formatTime(state.configuredSeconds),
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // Start button text (matching ActiveTimerScreen style)
                    Text(
                        text = "START",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryButton(onHistoryClick: () -> Unit) {
    FloatingActionButton(
        modifier = Modifier.size(68.dp),
        onClick = onHistoryClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = "History"
        )
    }
}
