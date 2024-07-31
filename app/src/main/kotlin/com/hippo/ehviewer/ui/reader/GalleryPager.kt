package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.unit.dp
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
    var showNavigationOverlay by remember {
        val showOnStart = Settings.showNavigationOverlayNewUser.value || Settings.showNavigationOverlayOnStart.value
        Settings.showNavigationOverlayNewUser.value = false
        mutableStateOf(showOnStart)
    }
    val regions = remember(navigation, invertMode) {
        if (firstLaunch) {
            firstLaunch = false
        } else {
            showNavigationOverlay = true
        }
        navigation.invertMode = TappingInvertMode.entries[invertMode]
        navigation.regions
    }
    val navigator by rememberUpdatedState(regions)
    if (isPagerType) {
        PagerViewer(
            pagerState = pagerState,
            isRtl = type == RIGHT_TO_LEFT,
            isVertical = type == VERTICAL,
            pageLoader = pageLoader,
            navigator = { navigator },
            onClick = { showNavigationOverlay = false },
            onSelectPage = onSelectPage,
            onMenuRegionClick = onMenuRegionClick,
            contentPadding = contentPadding,
            modifier = modifier,
        )
    } else {
        WebtoonViewer(
            lazyListState = lazyListState,
            withGaps = type == CONTINUOUS_VERTICAL,
            pageLoader = pageLoader,
            navigator = { navigator },
            onClick = { showNavigationOverlay = false },
            onSelectPage = onSelectPage,
            onMenuRegionClick = onMenuRegionClick,
            contentPadding = contentPadding,
            modifier = modifier,
        )
    }
    LaunchedEffect(isPagerType) {
        if (isPagerType) {
            pagerState.interactionSource
        } else {
            lazyListState.interactionSource
        }.interactions.collect {
            showNavigationOverlay = false
        }
    }
    NavigationOverlay(showNavigationOverlay, regions, modifier = Modifier.fillMaxSize())
}
