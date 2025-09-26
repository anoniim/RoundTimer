package net.solvetheriddle.roundtimer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * A scrollable dial component for selecting timer duration.
 * Displays the current value prominently with adjacent values faded.
 * 
 * @param currentSeconds The currently selected time in seconds
 * @param onValueChange Callback when the selected time changes
 * @param formatTime Function to format seconds into display string
 * @param minValue Minimum selectable value in seconds (default: 30)
 * @param maxValue Maximum selectable value in seconds (default: 600)
 * @param step Step size in seconds (default: 10)
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollableDial(
    currentSeconds: Int,
    onValueChange: (Int) -> Unit,
    formatTime: (Int) -> String,
    minValue: Int = 30,
    maxValue: Int = 600,
    step: Int = 10,
    modifier: Modifier = Modifier
) {
    // Calculate the list of all possible values
    val values = remember(minValue, maxValue, step) {
        val list = mutableListOf<Int>()
        var current = minValue
        while (current <= maxValue) {
            list.add(current)
            current += step
        }
        list
    }
    
    // Calculate current index based on current value
    val currentIndex = remember(currentSeconds, values) {
        values.indexOf(currentSeconds).takeIf { it >= 0 } ?: 0
    }
    
    // LazyListState to control scrolling
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentIndex - 1).coerceAtLeast(0)
    )
    
    // Coroutine scope for scrolling animations
    val coroutineScope = rememberCoroutineScope()
    
    // Snap behavior for smooth scrolling to items
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    // Track the centered item based on scroll position
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        // Calculate which item is most centered
        val centerOffset = listState.layoutInfo.viewportSize.height / 2
        var closestItemIndex = 0
        var closestDistance = Int.MAX_VALUE
        
        listState.layoutInfo.visibleItemsInfo.forEach { item ->
            val itemCenter = item.offset + (item.size / 2)
            val distance = abs(itemCenter - centerOffset)
            if (distance < closestDistance) {
                closestDistance = distance
                closestItemIndex = item.index
            }
        }
        
        // Update value if changed
        if (closestItemIndex in values.indices) {
            val newValue = values[closestItemIndex]
            if (newValue != currentSeconds) {
                onValueChange(newValue)
            }
        }
    }
    
    // Scroll to value when it changes externally
    LaunchedEffect(currentSeconds) {
        val targetIndex = values.indexOf(currentSeconds)
        if (targetIndex >= 0 && targetIndex != listState.firstVisibleItemIndex + 1) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index = (targetIndex - 1).coerceAtLeast(0),
                    scrollOffset = 0
                )
            }
        }
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Main scrollable list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    // Top gradient fade
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color.White.copy(alpha = 0.9f),
                                Color.White.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.25f
                        ),
                        blendMode = BlendMode.DstIn
                    )
                    // Bottom gradient fade
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.9f),
                                Color.White
                            ),
                            startY = size.height * 0.75f,
                            endY = size.height
                        ),
                        blendMode = BlendMode.DstIn
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = 80.dp) // Center the selected item
        ) {
            items(values.size) { index ->
                val value = values[index]
                val isCurrent = value == currentSeconds
                
                // Calculate distance from center for fading effect
                val centerIndex = currentIndex
                val distance = abs(index - centerIndex)
                
                // Determine visibility based on position
                // Show current, and adjacent values only if they exist
                val showPrevious = index == centerIndex - 1 && index >= 0
                val showNext = index == centerIndex + 1 && index < values.size
                val isVisible = isCurrent || showPrevious || showNext
                
                // Special handling for edge cases
                val atMinimum = currentSeconds == minValue
                val atMaximum = currentSeconds == maxValue
                
                // Don't show previous if at minimum, don't show next if at maximum
                val shouldDisplay = when {
                    isCurrent -> true
                    showPrevious && !atMinimum -> true
                    showNext && !atMaximum -> true
                    else -> false
                }
                
                // Alpha and scale based on distance from center
                val alpha by animateFloatAsState(
                    targetValue = when {
                        !shouldDisplay -> 0f
                        distance == 0 -> 1f
                        distance == 1 -> 0.35f
                        else -> 0f
                    },
                    label = "alpha"
                )
                
                val fontSize = when (distance) {
                    0 -> 72.sp  // Current value - large
                    1 -> 36.sp  // Adjacent values - smaller and faded
                    else -> 24.sp
                }
                
                val fontWeight = when (distance) {
                    0 -> FontWeight.Bold
                    else -> FontWeight.Normal
                }
                
                if (shouldDisplay) {
                    Text(
                        text = formatTime(value),
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .alpha(alpha)
                            .fillMaxWidth()
                            .padding(vertical = if (distance == 0) 8.dp else 4.dp)
                    )
                } else {
                    // Spacer for non-visible items to maintain consistent spacing
                    Spacer(modifier = Modifier.height(if (distance <= 1) 44.dp else 24.dp))
                }
            }
        }
        
        // Center selection indicators (subtle lines on either side of selected value)
        Row(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier
                    .width(30.dp)
                    .alpha(0.2f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(100.dp))
            HorizontalDivider(
                modifier = Modifier
                    .width(30.dp)
                    .alpha(0.2f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
