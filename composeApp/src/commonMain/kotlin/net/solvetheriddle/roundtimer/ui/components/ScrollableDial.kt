package net.solvetheriddle.roundtimer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collect
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
    
    // LazyListState to control scrolling - center the current item (add 1 for spacer)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentIndex + 1,
        initialFirstVisibleItemScrollOffset = 0
    )
    
    // Coroutine scope for scrolling animations
    val coroutineScope = rememberCoroutineScope()
    
    // Snap behavior for smooth scrolling to items
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    // Track the centered item based on scroll position
    LaunchedEffect(listState) {
        snapshotFlow { 
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@snapshotFlow 0
            
            // Dynamic target offset based on position in list
            val firstVisibleIndex = visibleItems.first().index
            val targetOffset = when {
                firstVisibleIndex == 0 -> {
                    // When at the beginning, use the first visible item's position
                    val firstItem = visibleItems.find { it.index == 0 }
                    firstItem?.let { it.offset + it.size / 2 } ?: (listState.layoutInfo.viewportSize.height * 0.3f).toInt()
                }
                else -> {
                    // Normal target at 30% from top
                    (listState.layoutInfo.viewportSize.height * 0.3f).toInt()
                }
            }
            
            // Find closest item to target position
            var closestItemIndex = 0
            var closestDistance = Int.MAX_VALUE
            
            visibleItems.forEach { item ->
                // Skip the spacer item (index 0)
                if (item.index > 0) {
                    val itemCenter = item.offset + (item.size / 2)
                    val distance = abs(itemCenter - targetOffset)
                    if (distance < closestDistance) {
                        closestDistance = distance
                        closestItemIndex = item.index - 1  // Adjust for spacer
                    }
                }
            }
            closestItemIndex
        }.collect { closestItemIndex ->
            // Update value if changed and valid
            if (closestItemIndex in values.indices) {
                val newValue = values[closestItemIndex]
                if (newValue != currentSeconds) {
                    onValueChange(newValue)
                }
            }
        }
    }
    
    // Scroll to value when it changes externally (but not from our own updates)
    var isScrolling by remember { mutableStateOf(false) }
    LaunchedEffect(nearestValidValue) {
        if (!isScrolling) {
            val targetIndex = values.indexOf(nearestValidValue)
            if (targetIndex >= 0 && abs(listState.firstVisibleItemIndex - (targetIndex + 1)) > 2) {
                coroutineScope.launch {
                    listState.animateScrollToItem(
                        index = targetIndex + 1,  // Add 1 for spacer
                        scrollOffset = 0
                    )
                }
            }
        }
    }
    
    // Track scrolling state
    LaunchedEffect(listState.isScrollInProgress) {
        isScrolling = listState.isScrollInProgress
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Calculate dynamic padding based on height
        val selectionOffsetRatio = 0.3f // Selection point at 30% from top
        
        // Main scrollable list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            flingBehavior = flingBehavior,
            // Dynamic padding based on selection position
            // Top padding = zero to allow first item to reach selection point
            // Bottom padding = 120dp works perfectly for max value (10:00)
            contentPadding = PaddingValues(top = 0.dp, bottom = 120.dp)
        ) {
            // Add empty spacer item at the beginning to allow first value to scroll down
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
            
            items(values.size) { index ->
                val value = values[index]
                
                DialItem(
                    value = value,
                    index = index,
                    listState = listState,
                    formatTime = formatTime,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    secondaryColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LazyItemScope.DialItem(
    value: Int,
    index: Int,
    listState: LazyListState,
    formatTime: (Int) -> String,
    primaryColor: Color,
    secondaryColor: Color
) {
    // Calculate item's position relative to the selection point (upper portion)
    val layoutInfo = listState.layoutInfo
    val viewportHeight = layoutInfo.viewportSize.height
    val targetOffset = (viewportHeight * 0.3f).toInt()
    
    // Find this item in the visible items
    val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
    
    // Calculate how close this item is to the selection point (0 = selected, 1 = far)
    val distanceFromCenter = itemInfo?.let { info ->
        val itemCenter = info.offset + (info.size / 2)
        val distance = abs(itemCenter - targetOffset).toFloat()
        // Normalize to 0-1 range where 0 is at target position
        (distance / (viewportHeight * 0.4f)).coerceIn(0f, 1f)
    } ?: 1f
    
    // Interpolate values based on distance from center
    val scale = 1f - (distanceFromCenter * 0.5f) // Scale from 1.0 to 0.5
    val fontSize = (72f - (44f * distanceFromCenter)).sp // Interpolate from 72sp to 28sp
    val alpha = 1f - (distanceFromCenter * 0.6f) // Alpha from 1.0 to 0.4
    
    // Interpolate color based on distance
    val textColor = lerp(primaryColor, secondaryColor, distanceFromCenter)
    
    // Font weight transitions
    val fontWeight = if (distanceFromCenter < 0.3f) FontWeight.Bold else FontWeight.Normal
    
    // Only show items that are reasonably close to center
    if (distanceFromCenter <= 1f) {
        Text(
            text = formatTime(value),
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(alpha)
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        )
    } else {
        // Spacer for items too far from center
        Spacer(modifier = Modifier.height(32.dp))
    }
}
