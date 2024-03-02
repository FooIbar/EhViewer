package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.gallery.PageLoader2
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType.CONTINUOUS_VERTICAL
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType.DEFAULT
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType.LEFT_TO_RIGHT
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType.RIGHT_TO_LEFT
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType.VERTICAL
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType.WEBTOON
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

@Composable
fun GalleryPager(
    type: ReadingModeType,
    pagerState: PagerState,
    lazyListState: LazyListState,
    pageLoader: PageLoader2,
    onSelectPage: (ReaderPage) -> Unit,
    onMenuRegionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = pageLoader.pages
    when (type) {
        DEFAULT, LEFT_TO_RIGHT, RIGHT_TO_LEFT -> {
            val zoomableState = rememberZoomableState(zoomSpec = zoomSpec)
            HorizontalPager(
                state = pagerState,
                modifier = modifier,
                reverseLayout = type == RIGHT_TO_LEFT,
                key = { it },
            ) { index ->
                val page = items[index]
                val status by page.status.collectAsState()
                val boxModifier = if (status == Page.State.READY) {
                    Modifier.zoomable(
                        state = zoomableState,
                        onClick = { onMenuRegionClick() },
                        onLongClick = { onSelectPage(page) },
                    )
                } else {
                    Modifier.combinedClickable(
                        interactionSource = null,
                        indication = null,
                        onClick = { onMenuRegionClick() },
                    ).fillMaxSize()
                }
                Box(
                    modifier = boxModifier,
                    contentAlignment = Alignment.Center,
                ) {
                    PagerItem(
                        page = page,
                        pageLoader = pageLoader,
                        zoomableState = zoomableState,
                        webtoon = false,
                    )
                }
            }
        }
        VERTICAL -> {
            val zoomableState = rememberZoomableState(zoomSpec = zoomSpec)
            VerticalPager(
                state = pagerState,
                modifier = modifier,
                key = { it },
            ) { index ->
                val page = items[index]
                val status by page.status.collectAsState()
                val boxModifier = if (status == Page.State.READY) {
                    Modifier.zoomable(
                        state = zoomableState,
                        onClick = { onMenuRegionClick() },
                        onLongClick = { onSelectPage(page) },
                    )
                } else {
                    Modifier.combinedClickable(
                        interactionSource = null,
                        indication = null,
                        onClick = { onMenuRegionClick() },
                    ).fillMaxSize()
                }
                Box(
                    modifier = boxModifier,
                    contentAlignment = Alignment.Center,
                ) {
                    PagerItem(
                        page = page,
                        pageLoader = pageLoader,
                        zoomableState = zoomableState,
                        webtoon = false,
                    )
                }
            }
        }
        WEBTOON, CONTINUOUS_VERTICAL -> {
            val zoomableState = rememberZoomableState(zoomSpec = zoomSpec)
            LazyColumn(
                modifier = modifier.zoomable(
                    state = zoomableState,
                    onClick = { onMenuRegionClick() },
                    onLongClick = { ofs ->
                        val info = lazyListState.layoutInfo.visibleItemsInfo.find { info ->
                            info.offset <= ofs.y && info.offset + info.size > ofs.y
                        }
                        if (info == null && type == CONTINUOUS_VERTICAL) {
                            // Maybe user long-click on gaps? ¯\_(ツ)_/¯
                            return@zoomable
                        }
                        info ?: error("Internal error finding long click item!!! offset:$ofs")
                        onSelectPage(items[info.index])
                    },
                ),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(if (type != WEBTOON) 15.dp else 0.dp),
            ) {
                items(items, key = { it.index }) { page ->
                    PagerItem(
                        page = page,
                        pageLoader = pageLoader,
                        zoomableState = zoomableState,
                        webtoon = true,
                    )
                }
            }
        }
    }
}

private val zoomSpec = ZoomSpec(maxZoomFactor = 3f)
