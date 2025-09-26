package net.solvetheriddle.roundtimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A styled card component that matches the design from ActiveTimerScreen.
 * 
 * This card features:
 * - Fixed size: 350dp x 350dp
 * - Rounded corners: 40dp
 * - Semi-transparent white background with 95% opacity
 * - Elevated shadow: 12dp
 * - 24dp padding around the card for shadow space
 * - 32dp internal padding
 * 
 * @param modifier Optional modifier to apply to the outer container
 * @param animatedSize Optional animated size for dynamic sizing (defaults to 350dp)
 * @param content The content to display inside the card
 */
@Composable
fun StyledCard(
    modifier: Modifier = Modifier,
    animatedSize: androidx.compose.ui.unit.Dp = 350.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    // Card container with padding to prevent shadow clipping
    Box(
        modifier = modifier
            .width(animatedSize)
            .height(animatedSize)
            .padding(24.dp), // Add padding to provide space for shadow
    ) {
        // Main content card
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(40.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}