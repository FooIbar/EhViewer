package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.util.VelocityTrackerAddPointsFix
import androidx.compose.ui.res.colorResource
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.archiveFile
import com.hippo.ehviewer.gallery.ArchivePageLoader
import com.hippo.ehviewer.gallery.EhPageLoader
import com.hippo.ehviewer.ui.LockDrawer
import com.hippo.ehviewer.ui.composing
import com.hippo.ehviewer.ui.tools.Deferred
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.ui.reader.PageIndicatorText
import eu.kanade.tachiyomi.ui.reader.ReaderAppBars
import eu.kanade.tachiyomi.ui.reader.ReaderContentOverlay
import eu.kanade.tachiyomi.ui.reader.ReaderPageSheetMeta
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import moe.tarsin.kt.unreachable

@Destination
@Composable
fun ReaderScreen(info: BaseGalleryInfo, page: Int = -1, navigator: DestinationsNavigator) = composing(navigator) {
    // Workaround for incorrect fling velocity on zoom gesture end
    // https://github.com/saket/telephoto/issues/71
    DisposableEffect(Unit) {
        VelocityTrackerAddPointsFix = false
        onDispose {
            VelocityTrackerAddPointsFix = true
        }
    }

    LockDrawer(true)
    ConfigureKeepScreenOn()
    val pageLoader = remember {
        val archive = DownloadManager.getDownloadInfo(info.gid)?.archiveFile
        archive?.let { ArchivePageLoader(it, info.gid, page) } ?: EhPageLoader(info, page)
    }
    LaunchedEffect(Unit) {
        Settings.cropBorder.changesFlow().collect {
            pageLoader.restart()
        }
    }
    DisposableEffect(pageLoader) {
        pageLoader.start()
        onDispose {
            pageLoader.stop()
        }
    }
    val showSeekbar by Settings.showReaderSeekbar.collectAsState()
    val readingMode by Settings.readingMode.collectAsState { ReadingModeType.fromPreference(it) }
    Deferred({ pageLoader.awaitReady() }) {
        val lazyListState = rememberLazyListState()
        val pagerState = rememberPagerState { pageLoader.size }
        val syncState = rememberSliderPagerDoubleSyncState(lazyListState, pagerState, pageLoader)
        syncState.Sync()
        Box {
            var appbarVisible by remember { mutableStateOf(false) }
            val bgColor by collectBackgroundColorAsState()
            GalleryPager(
                type = readingMode,
                pagerState = pagerState,
                lazyListState = lazyListState,
                pageLoader = pageLoader,
                onSelectPage = { page ->
                    if (Settings.readerLongTapAction.value) {
                        launch {
                            dialog { cont ->
                                fun dispose() = cont.resume(Unit)
                                val state = rememberModalBottomSheetState()
                                ModalBottomSheet(
                                    onDismissRequest = { dispose() },
                                    sheetState = state,
                                    windowInsets = WindowInsets(0),
                                ) {
                                    ReaderPageSheetMeta(
                                        retry = { pageLoader.retryPage(page.index) },
                                        retryOrigin = { pageLoader.retryPage(page.index, true) },
                                        share = { launchIO { with(pageLoader) { shareImage(page, info) } } },
                                        copy = { launchIO { with(pageLoader) { copy(page) } } },
                                        save = { launchIO { with(pageLoader) { save(page) } } },
                                        saveTo = { launchIO { with(pageLoader) { saveTo(page) } } },
                                        dismiss = { launch { state.hide().also { dispose() } } },
                                    )
                                    Spacer(modifier = Modifier.navigationBarsPadding())
                                }
                            }
                        }
                    }
                },
                onMenuRegionClick = { appbarVisible = !appbarVisible },
                modifier = Modifier.background(bgColor),
            )
            val brightness by Settings.customBrightness.collectAsState()
            val brightnessValue by Settings.customBrightnessValue.collectAsState()
            val colorOverlayEnabled by Settings.colorFilter.collectAsState()
            val colorOverlay by Settings.colorFilterValue.collectAsState()
            val colorOverlayMode by Settings.colorFilterMode.collectAsState {
                when (it) {
                    0 -> BlendMode.SrcOver
                    1 -> BlendMode.Multiply
                    2 -> BlendMode.Screen
                    3 -> BlendMode.Overlay
                    4 -> BlendMode.Lighten
                    5 -> BlendMode.Darken
                    else -> unreachable()
                }
            }
            ReaderContentOverlay(
                brightness = { brightnessValue }.takeIf { brightness && brightnessValue < 0 },
                color = { colorOverlay }.takeIf { colorOverlayEnabled },
                colorBlendMode = colorOverlayMode,
            )
            val showPageNumber by Settings.showPageNumber.collectAsState()
            if (showPageNumber) {
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodySmall) {
                    PageIndicatorText(
                        currentPage = syncState.sliderValue,
                        totalPages = pageLoader.size,
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
                    )
                }
            }
            ReaderAppBars(
                visible = appbarVisible,
                isRtl = readingMode == ReadingModeType.RIGHT_TO_LEFT,
                showSeekBar = showSeekbar,
                currentPage = syncState.sliderValue,
                totalPages = pageLoader.size,
                onSliderValueChange = { syncState.sliderScrollTo(it + 1) },
                onClickSettings = {
                    launch {
                        dialog { cont ->
                            fun dispose() = cont.resume(Unit)
                            ModalBottomSheet(
                                onDismissRequest = { dispose() },
                                dragHandle = null,
                                windowInsets = WindowInsets(0),
                            ) {
                                SettingsPager(modifier = Modifier.fillMaxSize())
                                Spacer(modifier = Modifier.navigationBarsPadding())
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun collectBackgroundColorAsState(): State<Color> {
    val grey = colorResource(R.color.reader_background_dark)
    val dark = isSystemInDarkTheme()
    return Settings.readerTheme.collectAsState { theme ->
        when (theme) {
            0 -> Color.White
            2 -> grey
            3 -> if (dark) grey else Color.White
            else -> Color.Black
        }
    }
}
