package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFold
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.gallery.PageLoader2
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType.CONTINUOUS_VERTICAL
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType.RIGHT_TO_LEFT
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType.VERTICAL
import eu.kanade.tachiyomi.ui.reader.setting.TappingInvertMode
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun GalleryPager(
    type: ReadingModeType,
    pagerState: PagerState,
    lazyListState: LazyListState,
    pageLoader: PageLoader2,
    showNavigationOverlay: Boolean,
    onNavigationModeChange: () -> Unit,
    onSelectPage: (ReaderPage) -> Unit,
    onMenuRegionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPagerType = !ReadingModeType.isWebtoon(type)
    val pagerNavigation by Settings.readerPagerNav.collectAsState()
    val pagerInvertMode by Settings.readerPagerNavInverted.collectAsState()
    val webtoonNavigation by Settings.readerWebtoonNav.collectAsState()
    val webtoonInvertMode by Settings.readerWebtoonNavInverted.collectAsState()
    val navigationType = if (isPagerType) pagerNavigation else webtoonNavigation
    val navigation = remember(navigationType, type) {
        ViewerNavigation.fromPreference(navigationType, ReadingModeType.isVertical(type))
    }
    val invertMode = if (isPagerType) pagerInvertMode else webtoonInvertMode
    var firstLaunch by remember { mutableStateOf(true) }
    val regions = remember(navigation, invertMode) {
        if (firstLaunch) {
            firstLaunch = false
        } else {
            onNavigationModeChange()
        }
        navigation.invertMode = TappingInvertMode.entries[invertMode]
        navigation.regions
    }
    val navigator by rememberUpdatedState(regions)
    if (isPagerType) {
        val channel = remember { Channel<Float>(Channel.CONFLATED) }
        LaunchedEffect(channel) {
            channel.receiveAsFlow().collectLatest { delta ->
                if (delta != 0f) {
                    if (delta < 0) pagerState.moveToNext() else pagerState.moveToPrevious()
                }
            }
        }
        PagerViewer(
            pagerState = pagerState,
            isRtl = type == RIGHT_TO_LEFT,
            isVertical = type == VERTICAL,
            pageLoader = pageLoader,
            navigator = { navigator },
            onSelectPage = onSelectPage,
            onMenuRegionClick = onMenuRegionClick,
            modifier = modifier.pointerInput(channel) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitScrollEvent()
                        val delta = calculateMouseWheelScroll(event)
                        channel.trySend(delta)
                    }
                }
            },
        )
    } else {
        WebtoonViewer(
            lazyListState = lazyListState,
            withGaps = type == CONTINUOUS_VERTICAL,
            pageLoader = pageLoader,
            navigator = { navigator },
            onSelectPage = onSelectPage,
            onMenuRegionClick = onMenuRegionClick,
            modifier = modifier,
        )
    }
    NavigationOverlay(showNavigationOverlay, regions, modifier = Modifier.fillMaxSize())
}

private suspend fun AwaitPointerEventScope.awaitScrollEvent(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent()
    } while (event.type != PointerEventType.Scroll)
    return event
}

private fun Density.calculateMouseWheelScroll(event: PointerEvent): Float {
    // 64 dp value is taken from ViewConfiguration.java, replace with better solution
    return event.changes.fastFold(0f) { acc, c -> acc + c.scrollDelta.y } * -64.dp.toPx()
}
