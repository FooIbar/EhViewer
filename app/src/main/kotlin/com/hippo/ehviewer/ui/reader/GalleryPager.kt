package com.hippo.ehviewer.ui.reader

import android.graphics.PointF
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.gallery.PageLoader2
import com.hippo.ehviewer.ui.main.plus
import eu.kanade.tachiyomi.data.preference.PreferenceValues.TappingInvertMode
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType.CONTINUOUS_VERTICAL
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType.RIGHT_TO_LEFT
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType.VERTICAL
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType.WEBTOON
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NavigationRegion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.DoubleClickToZoomListener
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.ZoomableState
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
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val scope = rememberCoroutineScope()
    val items = pageLoader.pages
    val isPagerType = !ReadingModeType.isWebtoon(type)
    val pagerNavigation by Settings.readerPagerNav.collectAsState()
    val pagerInvertMode by Settings.readerPagerNavInverted.collectAsState {
        TappingInvertMode.entries[it]
    }
    val webtoonNavigation by Settings.readerWebtoonNav.collectAsState()
    val webtoonInvertMode by Settings.readerWebtoonNavInverted.collectAsState {
        TappingInvertMode.entries[it]
    }
    val navigation = if (isPagerType) {
        pagerNavigation
    } else {
        webtoonNavigation
    }
    val navigator = remember(navigation, type) {
        ViewerNavigation.fromPreference(navigation, ReadingModeType.isVertical(type))
    }
    navigator.invertMode = if (isPagerType) {
        pagerInvertMode
    } else {
        webtoonInvertMode
    }
    val navigatorState = rememberUpdatedState(navigator)
    if (isPagerType) {
        val isVertical = type == VERTICAL
        val scaleType by Settings.imageScaleType.collectAsState()
        val landscapeZoom by Settings.landscapeZoom.collectAsState()
        val alignment by Settings.zoomStart.collectAsState(type) {
            Alignment.fromPreferences(it, type)
        }
        val layoutSize by remember(pagerState) {
            derivedStateOf {
                pagerState.layoutInfo.viewportSize.toSize()
            }
        }
        if (isVertical) {
            VerticalPager(
                state = pagerState,
                modifier = modifier,
                contentPadding = contentPadding,
                key = { it },
            ) { index ->
                val page = items[index]
                PageContainer(
                    page = page,
                    pageLoader = pageLoader,
                    isRtl = false,
                    scaleType = scaleType,
                    landscapeZoom = landscapeZoom,
                    alignment = alignment,
                    layoutSize = layoutSize,
                    navigatorState = navigatorState,
                    pagerState = pagerState,
                    onSelectPage = onSelectPage,
                    onMenuRegionClick = onMenuRegionClick,
                    scope = scope,
                )
            }
        } else {
            val isRtl = type == RIGHT_TO_LEFT
            val isRtlLayout = LocalLayoutDirection.current == LayoutDirection.Rtl
            HorizontalPager(
                state = pagerState,
                modifier = modifier,
                contentPadding = contentPadding,
                reverseLayout = isRtl xor isRtlLayout,
                key = { it },
            ) { index ->
                val page = items[index]
                PageContainer(
                    page = page,
                    pageLoader = pageLoader,
                    isRtl = isRtl,
                    scaleType = scaleType,
                    landscapeZoom = landscapeZoom,
                    alignment = alignment,
                    layoutSize = layoutSize,
                    navigatorState = navigatorState,
                    pagerState = pagerState,
                    onSelectPage = onSelectPage,
                    onMenuRegionClick = onMenuRegionClick,
                    scope = scope,
                )
            }
        }
    } else {
        val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec)
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
                            val size = lazyListState.layoutInfo.viewportSize
                            val pos = PointF(it.x / size.width, it.y / size.height)
                            val distance = size.height * 0.75f
                            when (navigatorState.value.getAction(pos)) {
                                NavigationRegion.MENU -> onMenuRegionClick()
                                NavigationRegion.NEXT, NavigationRegion.RIGHT -> performScrollBy(distance)
                                NavigationRegion.PREV, NavigationRegion.LEFT -> performScrollBy(-distance)
                            }
                        }
                    }
                },
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
                onDoubleClick = DoubleTapZoom,
            ),
            state = lazyListState,
            contentPadding = contentPadding + PaddingValues(horizontal = sidePadding),
            verticalArrangement = Arrangement.spacedBy(if (type != WEBTOON) 15.dp else 0.dp),
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
    val showOnStart = remember {
        Settings.showNavigationOverlayNewUser.value || Settings.showNavigationOverlayOnStart.value
    }
    NavigationOverlay(
        navigator,
        navigator.invertMode,
        showOnStart,
        onDismiss = { Settings.showNavigationOverlayNewUser.value = false },
    )
}

@Composable
private fun PageContainer(
    page: ReaderPage,
    pageLoader: PageLoader2,
    isRtl: Boolean,
    scaleType: Int,
    landscapeZoom: Boolean,
    alignment: Alignment.Horizontal,
    layoutSize: Size,
    navigatorState: State<ViewerNavigation>,
    pagerState: PagerState,
    onSelectPage: (ReaderPage) -> Unit,
    onMenuRegionClick: () -> Unit,
    scope: CoroutineScope,
) {
    @Suppress("NAME_SHADOWING")
    val isRtl by rememberUpdatedState(isRtl)
    val zoomableState = rememberZoomableState(zoomSpec = PagerZoomSpec)
    val status by page.status.collectAsState()
    if (status == Page.State.READY) {
        val size = page.image!!.rect.size.toSize()
        val contentScale = ContentScale.fromPreferences(scaleType, size, layoutSize)
        zoomableState.contentScale = contentScale
        LaunchedEffect(size, contentScale, alignment) {
            val contentSize = if (contentScale is FixedScale) { // Original
                size
            } else {
                size * contentScale.computeScaleFactor(size, layoutSize)
            }
            val horizontalAlignment = if (contentSize.width > layoutSize.width) {
                alignment
            } else {
                Alignment.CenterHorizontally
            }
            val verticalAlignment = if (contentSize.height > layoutSize.height) {
                Alignment.Top
            } else {
                Alignment.CenterVertically
            }
            zoomableState.contentAlignment = horizontalAlignment + verticalAlignment
        }
        LaunchedEffect(size) {
            val contentLocation = ZoomableContentLocation.scaledInsideAndCenterAligned(size)
            zoomableState.setContentLocation(contentLocation)
        }
        if (landscapeZoom && contentScale == ContentScale.Fit && size.width > size.height) {
            LaunchedEffect(alignment) {
                val zoomFraction = snapshotFlow { zoomableState.zoomFraction }.first { it != null }
                if (zoomFraction == 0f) {
                    delay(500)
                    val contentSize = zoomableState.transformedContentBounds.size
                    val scale = ContentScale.FillHeight.computeScaleFactor(contentSize, layoutSize)
                    val targetScale = scale.scaleX.coerceAtMost(zoomableState.zoomSpec.maxZoomFactor)
                    val offset = alignment.align(0, layoutSize.width.toInt(), LayoutDirection.Ltr)
                    zoomableState.zoomTo(targetScale, Offset(offset.toFloat(), 0f))
                }
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PagerItem(
            page = page,
            pageLoader = pageLoader,
            contentScale = ContentScale.Inside,
            modifier = Modifier.zoomable(
                state = zoomableState,
                onClick = {
                    scope.launch {
                        with(pagerState) {
                            with(zoomableState) {
                                val pos = PointF(it.x / layoutSize.width, it.y / layoutSize.height)
                                val distance = layoutSize.width
                                when (navigatorState.value.getAction(pos)) {
                                    NavigationRegion.MENU -> onMenuRegionClick()
                                    NavigationRegion.NEXT -> {
                                        val canPan = if (isRtl) panRight(distance) else panLeft(distance)
                                        if (!canPan) {
                                            moveToNext()
                                        }
                                    }

                                    NavigationRegion.PREV -> {
                                        val canPan = if (isRtl) panLeft(distance) else panRight(distance)
                                        if (!canPan) {
                                            moveToPrevious()
                                        }
                                    }

                                    NavigationRegion.RIGHT -> {
                                        if (!panLeft(distance)) {
                                            if (isRtl) moveToPrevious() else moveToNext()
                                        }
                                    }

                                    NavigationRegion.LEFT -> {
                                        if (!panRight(distance)) {
                                            if (isRtl) moveToNext() else moveToPrevious()
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                onLongClick = { onSelectPage(page) },
                onDoubleClick = DoubleTapZoom,
            ),
        )
    }
}

suspend fun ZoomableState.panLeft(distance: Float): Boolean {
    val canPan = Settings.navigateToPan.value && transformedContentBounds.right > distance
    if (canPan) {
        panBy(Offset(-distance, 0f))
    }
    return canPan
}

suspend fun ZoomableState.panRight(distance: Float): Boolean {
    val canPan = Settings.navigateToPan.value && transformedContentBounds.left < 0
    if (canPan) {
        panBy(Offset(distance, 0f))
    }
    return canPan
}

suspend fun PagerState.performScrollToPage(page: Int) {
    if (Settings.pageTransitions.value) {
        animateScrollToPage(page)
    } else {
        scrollToPage(page)
    }
}

suspend fun PagerState.moveToPrevious() {
    val target = currentPage - 1
    if (target >= 0) {
        performScrollToPage(target)
    }
}

suspend fun PagerState.moveToNext() {
    val target = currentPage + 1
    if (target < pageCount) {
        performScrollToPage(target)
    }
}

suspend fun LazyListState.performScrollBy(value: Float) {
    if (Settings.pageTransitions.value) {
        animateScrollBy(value)
    } else {
        scrollBy(value)
    }
}

object DoubleTapZoom : DoubleClickToZoomListener {
    override suspend fun onDoubleClick(state: ZoomableState, centroid: Offset) {
        if (Settings.doubleTapToZoom.value) {
            val zoomFraction = state.zoomFraction ?: return // Content isn't ready yet
            if (zoomFraction > 0.05f) {
                state.resetZoom()
            } else {
                // Workaround for https://github.com/saket/telephoto/issues/45
                state.zoomTo(
                    zoomFactor = state.contentTransformation.scaleMetadata.initialScale.scaleX * 2f,
                    centroid = centroid,
                )
            }
        }
    }
}

private val PagerZoomSpec = ZoomSpec(maxZoomFactor = 5f)
private val ZoomSpec = ZoomSpec(maxZoomFactor = 3f)
