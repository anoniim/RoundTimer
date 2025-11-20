package net.solvetheriddle.roundtimer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
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
    minValue: Int = 60,
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

    val pagerState = rememberPagerState(
        initialPage = values.indexOf(currentSeconds).coerceAtLeast(0),
        pageCount = { values.size }
    )
    LaunchedEffect(pagerState) {
        // Use settledPage to only trigger when animation completes, avoiding intermediate values
        snapshotFlow { pagerState.settledPage }.drop(1).collect { page ->
            onValueChange(values[page])
        }
    }

    LaunchedEffect(currentSeconds) {
        val page = values.indexOf(currentSeconds).coerceAtLeast(0)
        if (page != pagerState.currentPage) {
            pagerState.animateScrollToPage(page)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.sizeIn(maxWidth = 180.dp, maxHeight = 180.dp),
            pageSize = PageSize.Fixed(64.dp),
            contentPadding = PaddingValues(vertical = 60.dp)
        ) { page ->
            val pageOffset = with(pagerState) {
                (currentPage - page) + currentPageOffsetFraction
            }
            val scale = lerp(1f, 0.75f, abs(pageOffset))
            val alpha = lerp(1f, 0.25f, abs(pageOffset))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = alpha
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatTime(values[page]),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
