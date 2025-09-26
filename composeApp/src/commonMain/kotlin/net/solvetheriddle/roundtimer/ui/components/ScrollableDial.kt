package net.solvetheriddle.roundtimer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
        buildList {
            var current = minValue
            while (current <= maxValue) {
                add(current)
                current += step
            }
        }
    }
    
    // Ensure current value is valid and in range
    val validCurrentSeconds = currentSeconds.coerceIn(minValue, maxValue)
    val nearestValidValue = values.minByOrNull { abs(it - validCurrentSeconds) } ?: values.first()
    
    // Calculate current index based on current value
    val currentIndex = remember(nearestValidValue, values) {
        values.indexOf(nearestValidValue).coerceAtLeast(0)
    }
    
    // LazyListState to control scrolling - start with current item in view
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0)
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
            if (newValue != nearestValidValue) {
                onValueChange(newValue)
            }
        }
    }
    
    // Scroll to value when it changes externally
    LaunchedEffect(nearestValidValue) {
        val targetIndex = values.indexOf(nearestValidValue)
        if (targetIndex >= 0) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index = targetIndex,
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
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = 100.dp) // Center the selected item
        ) {
            items(values.size) { index ->
                val value = values[index]
                val isCurrent = value == nearestValidValue
                
                // Calculate distance from center for fading effect
                val centerIndex = currentIndex
                val distance = abs(index - centerIndex)
                
                // Determine what to show based on distance
                val fontSize = when (distance) {
                    0 -> 72.sp  // Current value - large
                    1 -> 36.sp  // Adjacent values - smaller
                    else -> 20.sp // Other values - very small
                }
                
                val fontWeight = when (distance) {
                    0 -> FontWeight.Bold
                    else -> FontWeight.Normal
                }
                
                val alpha = when (distance) {
                    0 -> 1f
                    1 -> 0.4f  
                    2 -> 0.15f
                    else -> 0f
                }
                
                // Always render text items to maintain scrolling consistency
                Text(
                    text = formatTime(value),
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(alpha)
                        .fillMaxWidth()
                        .padding(vertical = if (distance == 0) 8.dp else 4.dp)
                )
            }
        }
    }
}
