package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.gallery.PageLoader2
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

@Stable
class SliderPagerDoubleSync(
    private val lazyListState: LazyListState,
    private val pagerState: PagerState,
    private val pageLoader: PageLoader2,
) {
    private var requestScroll = true
    private var sliderFollowPager = true
    var sliderValue by mutableIntStateOf(pageLoader.startPage + 1)
        private set

    fun sliderScrollTo(index: Int) {
        sliderFollowPager = false
        sliderValue = index.coerceIn(1, pageLoader.size)
    }

    @Composable
    fun Sync(webtoon: Boolean) {
        val fling by lazyListState.interactionSource.collectIsDraggedAsState()
        val pagerFling by pagerState.interactionSource.collectIsDraggedAsState()
        if (fling || pagerFling) sliderFollowPager = true
        val currentIndexFlow = remember(webtoon) {
            requestScroll = true
            if (webtoon) {
                snapshotFlow {
                    with(lazyListState.layoutInfo) {
                        visibleItemsInfo.lastOrNull {
                            it.offset <= maxOf(viewportStartOffset, viewportEndOffset - it.size)
                        }?.index
                    }
                }.filterNotNull()
            } else {
                snapshotFlow { pagerState.currentPage }
            }
        }
        LaunchedEffect(currentIndexFlow) {
            currentIndexFlow.collect { index ->
                if (sliderFollowPager && !requestScroll) {
                    sliderValue = index + 1
                    pageLoader.startPage = index
                }
            }
        }
        LaunchedEffect(webtoon) {
            snapshotFlow { sliderValue - 1 }.collectLatest { index ->
                if (sliderFollowPager && !requestScroll) return@collectLatest
                if (webtoon) {
                    if (requestScroll) {
                        lazyListState.requestScrollToItem(index)
                        requestScroll = false
                    } else {
                        if (smoothScroll((index - lazyListState.firstVisibleItemIndex))) {
                            lazyListState.animateScrollToItem(index)
                        } else {
                            lazyListState.scrollToItem(index)
                        }
                    }
                } else {
                    if (requestScroll) {
                        pagerState.requestScrollToPage(index)
                        requestScroll = false
                    } else {
                        if (smoothScroll((index - pagerState.currentPage))) {
                            pagerState.animateScrollToPage(index)
                        } else {
                            pagerState.scrollToPage(index)
                        }
                    }
                }
                pageLoader.startPage = index
            }
        }
    }
}

@Stable
@Composable
fun rememberSliderPagerDoubleSyncState(
    lazyListState: LazyListState,
    pagerState: PagerState,
    pageLoader: PageLoader2,
): SliderPagerDoubleSync = remember {
    SliderPagerDoubleSync(lazyListState, pagerState, pageLoader)
}

private fun smoothScroll(distance: Int) =
    distance.absoluteValue < SMOOTH_SCROLL_THRESHOLD && Settings.pageTransitions.value

private const val SMOOTH_SCROLL_THRESHOLD = 50
