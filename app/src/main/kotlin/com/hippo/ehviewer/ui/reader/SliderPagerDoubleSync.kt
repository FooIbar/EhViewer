package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull

class SliderPagerDoubleSync(private val lazyListState: LazyListState) {
    private var sliderFollowPager by mutableStateOf(true)
    var sliderValue by mutableIntStateOf(1)

    fun sliderScrollTo(index: Int) {
        sliderFollowPager = false
        sliderValue = index
    }

    @Composable
    fun Sync() {
        val fling by lazyListState.interactionSource.collectIsDraggedAsState()
        if (fling) sliderFollowPager = true
        if (sliderFollowPager) {
            LaunchedEffect(Unit) {
                snapshotFlow {
                    lazyListState.layoutInfo
                }.mapNotNull { info ->
                    info.visibleItemsInfo.lastOrNull()
                }.collect { info ->
                    sliderValue = info.index + 1
                }
            }
        } else {
            LaunchedEffect(Unit) {
                snapshotFlow { sliderValue - 1 }.collectLatest { index ->
                    lazyListState.animateScrollToItem(index)
                }
            }
        }
    }
}
