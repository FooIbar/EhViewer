package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.gallery.PageLoader2
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.NavigationRegions
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NavigationRegion
import eu.kanade.tachiyomi.ui.reader.viewer.getAction
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

@Composable
fun PagerViewer(
    pagerState: PagerState,
    isRtl: Boolean,
    isVertical: Boolean,
    pageLoader: PageLoader2,
    navigator: () -> NavigationRegions,
    onClick: () -> Unit,
    onSelectPage: (ReaderPage) -> Unit,
    onMenuRegionClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val items = pageLoader.pages
    val scaleType by Settings.imageScaleType.collectAsState()
    val landscapeZoom by Settings.landscapeZoom.collectAsState()
    val zoomStart by Settings.zoomStart.collectAsState()
    val alignment = Alignment.fromPreferences(zoomStart, isRtl, isVertical)
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
                navigator = navigator,
                pagerState = pagerState,
                onSelectPage = onSelectPage,
                onClick = onClick,
                onMenuRegionClick = onMenuRegionClick,
                scope = scope,
            )
        }
    } else {
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
                navigator = navigator,
                pagerState = pagerState,
                onSelectPage = onSelectPage,
                onClick = onClick,
                onMenuRegionClick = onMenuRegionClick,
                scope = scope,
            )
        }
    }
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
    navigator: () -> NavigationRegions,
    pagerState: PagerState,
    onClick: () -> Unit,
    onSelectPage: (ReaderPage) -> Unit,
    onMenuRegionClick: () -> Unit,
    scope: CoroutineScope,
) {
    @Suppress("NAME_SHADOWING")
    val isRtl by rememberUpdatedState(isRtl)
    val zoomableState = rememberZoomableState(zoomSpec = zoomSpec)
    val status by page.status.collectAsState()
    if (status == Page.State.READY && layoutSize != Size.Zero) {
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
                        onClick()
                        with(pagerState) {
                            with(zoomableState) {
                                val x = it.x / layoutSize.width
                                val y = it.y / layoutSize.height
                                val distance = layoutSize.width
                                when (navigator().getAction(x, y)) {
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

private suspend fun ZoomableState.panLeft(distance: Float): Boolean {
    val canPan = Settings.navigateToPan.value && transformedContentBounds.right.roundToInt() > distance
    if (canPan) {
        panBy(Offset(-distance, 0f))
    }
    return canPan
}

private suspend fun ZoomableState.panRight(distance: Float): Boolean {
    val canPan = Settings.navigateToPan.value && transformedContentBounds.left.roundToInt() < 0
    if (canPan) {
        panBy(Offset(distance, 0f))
    }
    return canPan
}

private val zoomSpec = me.saket.telephoto.zoomable.ZoomSpec(maxZoomFactor = 5f)
