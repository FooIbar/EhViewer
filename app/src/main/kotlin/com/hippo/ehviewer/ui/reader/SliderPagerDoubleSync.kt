package com.hippo.ehviewer.ui.reader

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
import com.hippo.ehviewer.gallery.PageLoader2
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

@Stable
class SliderPagerDoubleSync(
    private val lazyListState: LazyListState,
    private val pagerState: PagerState,
    private val pageLoader: PageLoader2,
) {
    private var sliderFollowPager by mutableStateOf(true)
    var sliderValue by mutableIntStateOf(pageLoader.startPage + 1)
        private set

    fun sliderScrollTo(index: Int) {
        sliderFollowPager = false
        sliderValue = index.coerceIn(1, pageLoader.size)
    }

    fun reset() {
        sliderFollowPager = true
    }

    @Composable
    fun Sync(webtoon: Boolean, onPageSelected: () -> Unit) {
        val currentIndexFlow = remember(webtoon) {
            val initialIndex = sliderValue - 1
            if (webtoon) {
                sliderFollowPager = lazyListState.firstVisibleItemIndex == initialIndex
                snapshotFlow {
                    with(lazyListState.layoutInfo) {
                        visibleItemsInfo.lastOrNull {
                            it.offset <= maxOf(viewportStartOffset, viewportEndOffset - it.size)
                        }?.index
                    }
                }.filterNotNull()
            } else {
                sliderFollowPager = pagerState.currentPage == initialIndex
                snapshotFlow { pagerState.currentPage }
            }
        }
        if (sliderFollowPager) {
            LaunchedEffect(currentIndexFlow) {
                currentIndexFlow.collect { index ->
                    sliderValue = index + 1
                    pageLoader.startPage = index
                    onPageSelected()
                }
            }
        } else {
            LaunchedEffect(webtoon) {
                snapshotFlow { sliderValue - 1 }.collectLatest { index ->
                    if (webtoon) {
                        lazyListState.scrollToItem(index)
                    } else {
                        pagerState.animateScrollToPage(index)
                    }
                    pageLoader.startPage = index
                }
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
