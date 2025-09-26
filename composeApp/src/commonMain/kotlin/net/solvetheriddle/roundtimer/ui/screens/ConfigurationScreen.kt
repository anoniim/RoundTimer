package net.solvetheriddle.roundtimer.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.solvetheriddle.roundtimer.model.TimerState
import net.solvetheriddle.roundtimer.platform.getStatusBarManager
import net.solvetheriddle.roundtimer.ui.components.ScrollableDial
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
                // Centered StyledCard with scrollable dial
                StyledCard(
                    modifier = Modifier,
                    animatedSize = 450.dp,
                    verticalArrangement = Arrangement.Top
                ) {
                    // Scrollable dial for time selection
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

                    // Start button
                    Button(
                        onClick = onStartTimer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "START",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
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
