package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.gallery.PageLoader2
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull

@Stable
class SliderPagerDoubleSync(
    private val lazyListState: LazyListState,
    private val pagerState: PagerState,
    private val pageLoader: PageLoader2,
) {
    private var sliderFollowPager by mutableStateOf(true)
    var sliderValue by mutableIntStateOf(1)

    fun sliderScrollTo(index: Int) {
        sliderFollowPager = false
        sliderValue = index
    }

    @Composable
    fun Sync() {
        val fling by lazyListState.interactionSource.collectIsDraggedAsState()
        val pagerFling by pagerState.interactionSource.collectIsDraggedAsState()
        if (fling || pagerFling) sliderFollowPager = true
        val startPage = remember { pageLoader.startPage }
        if (sliderFollowPager) {
            LaunchedEffect(Unit) {
                snapshotFlow {
                    lazyListState.layoutInfo
                }.mapNotNull { info ->
                    info.visibleItemsInfo.lastOrNull {
                        it.offset <= maxOf(info.viewportStartOffset, info.viewportEndOffset - it.size)
                    }
                }.collect { info ->
                    sliderValue = info.index + 1
                    pageLoader.startPage = info.index
                }
            }
            LaunchedEffect(Unit) {
                snapshotFlow {
                    pagerState.currentPage
                }.collect { index ->
                    sliderValue = index + 1
                    pageLoader.startPage = index
                }
            }
        } else {
            LaunchedEffect(Unit) {
                snapshotFlow { sliderValue - 1 }.collectLatest { index ->
                    val noAnim = (index - pagerState.currentPage).absoluteValue > SMOOTH_SCROLL_THRESHOLD || !Settings.pageTransitions.value
                    if (noAnim) {
                        pagerState.scrollToPage(index)
                    } else {
                        pagerState.animateScrollToPage(index)
                    }
                    pageLoader.startPage = index
                }
            }
            LaunchedEffect(Unit) {
                snapshotFlow { sliderValue - 1 }.collectLatest { index ->
                    val noAnim = (index - lazyListState.firstVisibleItemIndex).absoluteValue > SMOOTH_SCROLL_THRESHOLD || !Settings.pageTransitions.value
                    if (noAnim) {
                        lazyListState.scrollToItem(index)
                    } else {
                        lazyListState.animateScrollToItem(index)
                    }
                    pageLoader.startPage = index
                }
            }
        }
        LaunchedEffect(Unit) {
            pagerState.scrollToPage(startPage)
        }
        LaunchedEffect(Unit) {
            lazyListState.scrollToItem(startPage)
        }
    }
}

@Stable
@Composable
fun rememberSliderPagerDoubleSyncState(
    lazyListState: LazyListState,
    pagerState: PagerState,
    pageLoader: PageLoader2,
): SliderPagerDoubleSync {
    return remember {
        SliderPagerDoubleSync(lazyListState, pagerState, pageLoader)
    }
}

private const val SMOOTH_SCROLL_THRESHOLD = 50
