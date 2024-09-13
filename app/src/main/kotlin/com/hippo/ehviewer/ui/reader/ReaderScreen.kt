package com.hippo.ehviewer.ui.reader

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowInsetsControllerCompat
import arrow.core.Either
import arrow.core.raise.ensure
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.hasAds
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.archiveFile
import com.hippo.ehviewer.gallery.ArchivePageLoader
import com.hippo.ehviewer.gallery.EhPageLoader
import com.hippo.ehviewer.gallery.PageLoader2
import com.hippo.ehviewer.ui.composing
import com.hippo.ehviewer.ui.theme.EhTheme
import com.hippo.ehviewer.ui.tools.Await
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.EmptyWindowInsets
import com.hippo.ehviewer.util.displayString
import com.hippo.files.toOkioPath
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.PageIndicatorText
import eu.kanade.tachiyomi.ui.reader.ReaderAppBars
import eu.kanade.tachiyomi.ui.reader.ReaderContentOverlay
import eu.kanade.tachiyomi.ui.reader.ReaderPageSheetMeta
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import moe.tarsin.kt.unreachable

sealed interface ReaderScreenArgs : Parcelable {
    @Parcelize
    data class Gallery(val info: BaseGalleryInfo, val page: Int) : ReaderScreenArgs

    @Parcelize
    data class Archive(val uri: Uri) : ReaderScreenArgs
}

@Composable
private fun Background(
    color: Color,
    content: @Composable () -> Unit,
) = Box(Modifier.fillMaxSize().background(color), contentAlignment = Alignment.Center) {
    EhTheme(useDarkTheme = color != Color.White, content = content)
}

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.ReaderScreen(args: ReaderScreenArgs, navigator: DestinationsNavigator) = composing(navigator) {
    Await({ preparePageLoader(args) }) { pageLoader ->
        DisposableEffect(pageLoader) {
            pageLoader.start()
            onDispose {
                pageLoader.stop()
            }
        }
        val bgColor by collectBackgroundColorAsState()
        val readingFailed = stringResource(R.string.error_reading_failed)
        val uiController = rememberSystemUiController()
        DisposableEffect(uiController) {
            val lightStatusBar = uiController.statusBarDarkContentEnabled
            onDispose {
                uiController.statusBarDarkContentEnabled = lightStatusBar
            }
        }
        LaunchedEffect(uiController) {
            snapshotFlow { bgColor }.collect {
                uiController.statusBarDarkContentEnabled = it == Color.White
            }
        }
        Await(
            {
                Either.catch {
                    readingFailed.takeUnless { pageLoader.awaitReady() }
                }.fold({ it.displayString() }, { it })
            },
            placeholder = {
                Background(bgColor) {
                    CircularProgressIndicator()
                }
            },
        ) { error ->
            if (error == null) {
                val info = (args as? ReaderScreenArgs.Gallery)?.info
                key(pageLoader) {
                    ReaderScreen(pageLoader, info, navigator)
                }
            } else {
                Background(bgColor) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedVisibilityScope.ReaderScreen(pageLoader: PageLoader2, info: BaseGalleryInfo?, navigator: DestinationsNavigator) = composing(navigator) {
    ConfigureKeepScreenOn()
    LaunchedEffect(Unit) {
        val orientation = requestedOrientation
        Settings.orientationMode.valueFlow()
            .onCompletion { requestedOrientation = orientation }
            .collect { setOrientation(it) }
    }
    LaunchedEffect(pageLoader) {
        with(Settings) {
            merge(cropBorder.changesFlow(), stripExtraneousAds.changesFlow()).collect {
                pageLoader.restart()
            }
        }
    }
    val showSeekbar by Settings.showReaderSeekbar.collectAsState()
    val readingMode by Settings.readingMode.collectAsState { ReadingModeType.fromPreference(it) }
    val volumeKeysEnabled by Settings.readWithVolumeKeys.collectAsState()
    val fullscreen by Settings.fullscreen.collectAsState()
    val cutoutShort by Settings.cutoutShort.collectAsState()
    val uiController = rememberSystemUiController()
    DisposableEffect(uiController) {
        uiController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            uiController.isSystemBarsVisible = true
            uiController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }
    val lazyListState = rememberLazyListState(pageLoader.startPage)
    val pagerState = rememberPagerState(pageLoader.startPage) { pageLoader.size }
    val syncState = rememberSliderPagerDoubleSyncState(lazyListState, pagerState, pageLoader)
    Box {
        var appbarVisible by remember { mutableStateOf(false) }
        val bgColor by collectBackgroundColorAsState()
        val isWebtoon by rememberUpdatedState(ReadingModeType.isWebtoon(readingMode))
        syncState.Sync(isWebtoon) { appbarVisible = false }
        if (fullscreen) {
            LaunchedEffect(Unit) {
                snapshotFlow { appbarVisible }.collect {
                    uiController.isSystemBarsVisible = it
                }
            }
        }
        VolumeKeysHandler(
            enabled = { volumeKeysEnabled && !appbarVisible },
            movePrevious = {
                if (isWebtoon) lazyListState.scrollUp() else pagerState.moveToPrevious()
            },
            moveNext = {
                if (isWebtoon) lazyListState.scrollDown() else pagerState.moveToNext()
            },
        )
        var showNavigationOverlay by remember {
            val showOnStart = Settings.showNavigationOverlayNewUser.value || Settings.showNavigationOverlayOnStart.value
            Settings.showNavigationOverlayNewUser.value = false
            mutableStateOf(showOnStart)
        }
        val onSelectPage = { page: ReaderPage ->
            if (Settings.readerLongTapAction.value) {
                launch {
                    val pageBlocked = page.status.value == Page.State.BLOCKED
                    dialog { cont ->
                        fun dispose() = cont.resume(Unit)
                        val state = rememberModalBottomSheetState()
                        ModalBottomSheet(
                            onDismissRequest = { dispose() },
                            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
                            sheetState = state,
                            contentWindowInsets = { EmptyWindowInsets },
                        ) {
                            ReaderPageSheetMeta(
                                retry = { pageLoader.retryPage(page.index) },
                                retryOrigin = { pageLoader.retryPage(page.index, true) },
                                share = { launchIO { with(pageLoader) { shareImage(page, info) } } },
                                copy = { launchIO { with(pageLoader) { copy(page) } } },
                                save = { launchIO { with(pageLoader) { save(page) } } },
                                saveTo = { launchIO { with(pageLoader) { saveTo(page) } } },
                                showAds = { page.status.value = Page.State.READY }.takeIf { pageBlocked },
                                dismiss = { launch { state.hide().also { dispose() } } },
                            )
                        }
                    }
                }
            }
        }
        EhTheme(useDarkTheme = bgColor != Color.White) {
            val insets = if (fullscreen) {
                if (cutoutShort) {
                    EmptyWindowInsets
                } else {
                    WindowInsets.displayCutout
                }
            } else {
                WindowInsets.systemBars
            }
            GalleryPager(
                type = readingMode,
                pagerState = pagerState,
                lazyListState = lazyListState,
                pageLoader = pageLoader,
                showNavigationOverlay = showNavigationOverlay,
                onNavigationModeChange = { showNavigationOverlay = true },
                onSelectPage = onSelectPage,
                onMenuRegionClick = { appbarVisible = !appbarVisible },
                modifier = Modifier.background(bgColor).pointerInput(syncState) {
                    awaitEachGesture {
                        waitForUpOrCancellation()
                        syncState.reset()
                        showNavigationOverlay = false
                    }
                }.fillMaxSize().windowInsetsPadding(insets),
            )
        }
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
        if (brightness) {
            LaunchedEffect(Unit) {
                Settings.customBrightnessValue.valueFlow().sample(100)
                    .onCompletion { setCustomBrightnessValue(0) }
                    .collect { setCustomBrightnessValue(it) }
            }
        }
        val showPageNumber by Settings.showPageNumber.collectAsState()
        if (showPageNumber && !appbarVisible) {
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
                        var isColorFilter by remember { mutableStateOf(false) }
                        val scrim by animateColorAsState(
                            targetValue = if (isColorFilter) Color.Transparent else BottomSheetDefaults.ScrimColor,
                            label = "ScrimColor",
                        )
                        ModalBottomSheet(
                            onDismissRequest = { dispose() },
                            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
                            // Yeah, I know color state should not be read here, but we have to do it...
                            scrimColor = scrim,
                            dragHandle = null,
                            contentWindowInsets = { EmptyWindowInsets },
                        ) {
                            SettingsPager(modifier = Modifier.fillMaxSize()) { page ->
                                isColorFilter = page == 2
                                appbarVisible = !isColorFilter
                            }
                        }
                    }
                }
            },
        )
    }
}

context(Context, DialogState, DestinationsNavigator)
private suspend fun preparePageLoader(args: ReaderScreenArgs) = when (args) {
    is ReaderScreenArgs.Gallery -> {
        val info = args.info
        val page = args.page
        val archive = withIOContext { DownloadManager.getDownloadInfo(info.gid)?.archiveFile }
        if (archive != null) {
            ArchivePageLoader(archive, info.gid, page, info.hasAds)
        } else {
            EhPageLoader(info, page)
        }
    }
    is ReaderScreenArgs.Archive -> {
        ArchivePageLoader(args.uri.toOkioPath()) { invalidator ->
            awaitInputText(
                title = getString(R.string.archive_need_passwd),
                hint = getString(R.string.archive_passwd),
                onUserDismiss = { popBackStack() },
            ) { text ->
                ensure(text.isNotBlank()) { getString(R.string.passwd_cannot_be_empty) }
                ensure(invalidator(text)) { getString(R.string.passwd_wrong) }
            }
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
