package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.gallery.PageLoader2
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.NavigationRegions
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NavigationRegion
import eu.kanade.tachiyomi.ui.reader.viewer.getAction
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

@Composable
fun WebtoonViewer(
    lazyListState: LazyListState,
    withGaps: Boolean,
    pageLoader: PageLoader2,
    navigator: () -> NavigationRegions,
    onSelectPage: (ReaderPage) -> Unit,
    onMenuRegionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val items = pageLoader.pages
    val zoomableState = rememberZoomableState(zoomSpec = WebtoonZoomSpec)
    val density = LocalDensity.current
    val paddingPercent by Settings.webtoonSidePadding.collectAsState()
    val sidePadding by remember(density) {
        snapshotFlow {
            with(density) {
                (lazyListState.layoutInfo.viewportSize.width * paddingPercent / 100f).toDp()
            }
        }
    }.collectAsState(0.dp)
    LazyColumn(
        modifier = modifier.zoomable(
            state = zoomableState,
            onClick = {
                scope.launch {
                    with(lazyListState) {
                        val size = layoutInfo.viewportSize
                        val x = it.x / size.width
                        val y = it.y / size.height
                        when (navigator().getAction(x, y)) {
                            NavigationRegion.MENU -> onMenuRegionClick()
                            NavigationRegion.NEXT, NavigationRegion.RIGHT -> scrollDown()
                            NavigationRegion.PREV, NavigationRegion.LEFT -> scrollUp()
                        }
                    }
                }
            },
            onLongClick = { ofs ->
                val info = lazyListState.layoutInfo.visibleItemsInfo.find { info ->
                    info.offset <= ofs.y && info.offset + info.size > ofs.y
                }
                if (info != null) {
                    onSelectPage(items[info.index])
                }
            },
            onDoubleClick = DoubleTapZoom,
        ),
        state = lazyListState,
        contentPadding = PaddingValues(horizontal = sidePadding),
        verticalArrangement = Arrangement.spacedBy(if (withGaps) 15.dp else 0.dp),
    ) {
        items(items, key = { it.index }) { page ->
            PagerItem(
                page = page,
                pageLoader = pageLoader,
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}

private val WebtoonZoomSpec = ZoomSpec(maxZoomFactor = 3f)
