package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest

class SliderPagerDoubleSync(private val lazyListState: LazyListState) {
    private var sliderFollowPager by mutableStateOf(false)
    var sliderValue by mutableIntStateOf(0)

    fun sliderScrollTo(index: Int) {
        sliderFollowPager = false
        sliderValue = index
    }

    @Composable
    fun Sync() {
        val fling by lazyListState.interactionSource.collectIsDraggedAsState()
        val coroutineScope = rememberCoroutineScope()
        if (fling) sliderFollowPager = true
        if (sliderFollowPager) {
            LaunchedEffect(Unit) {
                snapshotFlow { lazyListState.layoutInfo }.collect { info ->
                    sliderValue = info.visibleItemsInfo.last().index
                }
            }
        } else {
            LaunchedEffect(Unit) {
                snapshotFlow { sliderValue }.collectLatest { index ->
                    lazyListState.animateScrollToItem(index)
                }
            }
        }
    }
}
