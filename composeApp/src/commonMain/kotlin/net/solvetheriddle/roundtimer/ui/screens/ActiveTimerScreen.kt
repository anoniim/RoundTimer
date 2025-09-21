package net.solvetheriddle.roundtimer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.solvetheriddle.roundtimer.model.TimerState
import net.solvetheriddle.roundtimer.platform.getStatusBarManager
import net.solvetheriddle.roundtimer.ui.components.StyledCard
import net.solvetheriddle.roundtimer.ui.theme.GreenBackground
import net.solvetheriddle.roundtimer.ui.theme.GreenBox
import net.solvetheriddle.roundtimer.ui.theme.OrangeBackground
import net.solvetheriddle.roundtimer.ui.theme.OrangeBox
import net.solvetheriddle.roundtimer.ui.theme.RedBackground
import net.solvetheriddle.roundtimer.ui.theme.RedBox

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
    val activeColor = when {
        state.isOvertime -> RedBackground // Red for overtime
        state.currentSeconds <= 36 -> RedBackground // Red
        state.currentSeconds <= 60 -> OrangeBackground // Orange
        else -> GreenBackground // Green
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        RedBackground.copy(alpha = 0.1f),
                        OrangeBackground.copy(alpha = 0.1f),
                        GreenBackground.copy(alpha = 0.1f),
                    )
                )
            ),
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
                    .background(activeColor.copy(alpha = 0.5f))
            )
        }

        // Styled card with animated size and pulse effect
        StyledCard(
            modifier = Modifier
                .scale(if (state.isOvertime) pulseScale else 1f)
                .clickable(
                    onClick = onStopTimer,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            size = boxWidth,
            content = {
                // Main countdown display
                Text(
                    text = formatTime(state.currentSeconds),
                    fontSize = 96.sp, // Extra large as per README (8xl equivalent)
                    fontWeight = FontWeight.Bold,
                    color = activeColor,
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
                        color = RedBackground,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Stop button
                Text(
                    text = "STOP",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            colors = CardDefaults.cardColors(containerColor = when (activeColor) {
                RedBackground -> RedBox
                OrangeBackground -> OrangeBox
                else -> GreenBox
            })
        )
    }
}
