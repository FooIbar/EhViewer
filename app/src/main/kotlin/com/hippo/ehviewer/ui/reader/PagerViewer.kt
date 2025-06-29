package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import arrow.core.partially1
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.gallery.Page
import com.hippo.ehviewer.gallery.PageLoader
import com.hippo.ehviewer.gallery.PageStatus
import com.hippo.ehviewer.gallery.statusObserved
import eu.kanade.tachiyomi.ui.reader.viewer.NavigationRegions
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NavigationRegion
import eu.kanade.tachiyomi.ui.reader.viewer.getAction
import kotlin.contracts.contract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

@Composable
fun PagerViewer(
    pagerState: PagerState,
    isRtl: Boolean,
    isVertical: Boolean,
    pageLoader: PageLoader,
    navigator: () -> NavigationRegions,
    onSelectPage: (Page) -> Unit,
    onMenuRegionClick: () -> Unit,
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
            beyondViewportPageCount = 1,
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
                onMenuRegionClick = onMenuRegionClick,
                scope = scope,
            )
        }
    } else {
        val isRtlLayout = LocalLayoutDirection.current == LayoutDirection.Rtl
        HorizontalPager(
            state = pagerState,
            modifier = modifier,
            beyondViewportPageCount = 1,
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
                onMenuRegionClick = onMenuRegionClick,
                scope = scope,
            )
        }
    }
}

@Composable
private fun PageContainer(
    page: Page,
    pageLoader: PageLoader,
    isRtl: Boolean,
    scaleType: Int,
    landscapeZoom: Boolean,
    alignment: Alignment.Horizontal,
    layoutSize: Size,
    navigator: () -> NavigationRegions,
    pagerState: PagerState,
    onSelectPage: (Page) -> Unit,
    onMenuRegionClick: () -> Unit,
    scope: CoroutineScope,
) {
    @Suppress("NAME_SHADOWING")
    val isRtl by rememberUpdatedState(isRtl)
    val zoomableState = rememberZoomableState(zoomSpec = PagerZoomSpec)
    val status = page.statusObserved
    if (status is PageStatus.Ready && layoutSize != Size.Zero) {
        val size = status.image.intrinsicSize.toSize()
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
                    val targetScale = scale.scaleX.coerceAtMost(zoomableState.zoomSpec.maximum.factor)
                    val offset = alignment.align(0, layoutSize.width.toInt(), LayoutDirection.Ltr)
                    zoomableState.zoomTo(targetScale, Offset(offset.toFloat(), 0f))
                }
            }
        }
    }
    val onLongClick = { _: Offset -> onSelectPage(page) }
    val onTap: ZoomableState?.(Offset) -> Unit = { offset ->
        scope.launch {
            with(pagerState) {
                // Don't use `layoutSize` as it may capture outdated value
                val size = layoutInfo.viewportSize.toSize()
                val (w, h) = size
                val (x, y) = offset
                val distance = size.width
                val bounds = size.toRect()
                when (navigator().getAction(Offset(x / w, y / h))) {
                    NavigationRegion.MENU -> onMenuRegionClick()
                    NavigationRegion.NEXT -> {
                        val canPan = if (isRtl) panRight(distance, bounds) else panLeft(distance, bounds)
                        if (!canPan) {
                            moveToNext()
                        }
                    }

                    NavigationRegion.PREV -> {
                        val canPan = if (isRtl) panLeft(distance, bounds) else panRight(distance, bounds)
                        if (!canPan) {
                            moveToPrevious()
                        }
                    }

                    NavigationRegion.RIGHT -> {
                        if (!panLeft(distance, bounds)) {
                            if (isRtl) moveToPrevious() else moveToNext()
                        }
                    }

                    NavigationRegion.LEFT -> {
                        if (!panRight(distance, bounds)) {
                            if (isRtl) moveToNext() else moveToPrevious()
                        }
                    }
                }
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PagerItem(
            page = page,
            pageLoader = pageLoader,
            contentScale = ContentScale.Inside,
            modifier = Modifier.pointerInput(onTap) {
                detectTapGestures(onLongPress = onLongClick, onTap = onTap.partially1(null))
            },
            contentModifier = Modifier.zoomable(
                state = zoomableState,
                onClick = onTap.partially1(zoomableState),
                onLongClick = onLongClick,
                onDoubleClick = DoubleTapZoom,
            ),
        )
    }
}

private suspend fun ZoomableState?.panLeft(distance: Float, bounds: Rect): Boolean = if (canPan { it.right - bounds.right }) {
    panBy(Offset(-distance, 0f))
    true
} else {
    false
}

private suspend fun ZoomableState?.panRight(distance: Float, bounds: Rect): Boolean = if (canPan { bounds.left - it.left }) {
    panBy(Offset(distance, 0f))
    true
} else {
    false
}

private inline fun ZoomableState?.canPan(getRemaining: (Rect) -> Float): Boolean {
    // TODO: Remove when K2 mode in IDE is stable
    contract {
        returns(true) implies (this@canPan != null)
    }
    return this != null && Settings.navigateToPan.value && getRemaining(transformedContentBounds) > 1f
}

private val PagerZoomSpec = ZoomSpec(maxZoomFactor = 5f)
