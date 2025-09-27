package net.solvetheriddle.roundtimer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.solvetheriddle.roundtimer.model.TimerState
import net.solvetheriddle.roundtimer.platform.getStatusBarManager
import net.solvetheriddle.roundtimer.ui.components.StyledCard

@Composable
fun ActiveTimerScreen(
    state: TimerState,
    onStopTimer: () -> Unit,
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
    // Pulse animation for overtime
    val pulseScale by animateFloatAsState(
        targetValue = if (state.isOvertime) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Size transition animation for overtime
    val boxSizeTransition = updateTransition(targetState = state.isOvertime, label = "boxSizeTransition")
    
    val boxWidth by boxSizeTransition.animateDp(
        transitionSpec = { spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow) },
        label = "boxWidth"
    ) { isOvertime -> if (isOvertime) 380.dp else 350.dp }
    
    val boxHeight by boxSizeTransition.animateDp(
        transitionSpec = { spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow) },
        label = "boxHeight"
    ) { isOvertime -> if (isOvertime) 420.dp else 350.dp }

    // Calculate progress fill percentage
    val progressPercentage = if (state.configuredTime > 0) {
        1f - (state.currentTime.toFloat() / state.configuredTime.toFloat())
    } else 1f

    // Determine background color based on remaining time
    val backgroundColor = when {
        state.isOvertime -> Color(0xFFDC2626) // Red for overtime
        state.currentSeconds <= 30 -> Color(0xFFDC2626) // Red
        state.currentSeconds <= 60 -> Color(0xFFF59E0B) // Orange
//        state.currentSeconds <= 60 -> Color(0xFFEAB308) // Yellow
        else -> Color(0xFF10B981) // Green
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Progress fill background
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background fill that rises from bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = progressPercentage)
                    .align(Alignment.BottomCenter)
                    .background(backgroundColor.copy(alpha = 0.5f))
            )
        }

        // Styled card with animated size and pulse effect
        StyledCard(
            modifier = Modifier
                .scale(if (state.isOvertime) pulseScale else 1f)
                .clickable(onClick = onStopTimer),
            size = boxWidth
        ) {
            // Main countdown display
            Text(
                text = formatTime(state.currentSeconds),
                fontSize = 96.sp, // Extra large as per README (8xl equivalent)
                fontWeight = FontWeight.Bold,
                color = if (state.isOvertime) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                lineHeight = 96.sp
            )

            // Overtime display
            if (state.isOvertime && state.overtimeTime > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "+${formatTime(state.overtimeSeconds)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFDC2626),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Stop button
            Text(
                text = "STOP",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
//                        color = MaterialTheme.colorScheme.onError
            )
        }
    }
}
