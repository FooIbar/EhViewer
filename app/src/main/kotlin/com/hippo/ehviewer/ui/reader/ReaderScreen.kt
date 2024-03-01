package com.hippo.ehviewer.ui.reader

import android.graphics.Rect
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import coil3.BitmapImage
import coil3.DrawableImage
import coil3.Image as CoilImage
import com.google.accompanist.drawablepainter.DrawablePainter
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.archiveFile
import com.hippo.ehviewer.gallery.ArchivePageLoader
import com.hippo.ehviewer.gallery.EhPageLoader
import com.hippo.ehviewer.ui.LockDrawer
import com.hippo.ehviewer.ui.tools.Deferred
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.ramcosta.composedestinations.annotation.Destination
import eu.kanade.tachiyomi.source.model.Page.State.DOWNLOAD_IMAGE
import eu.kanade.tachiyomi.source.model.Page.State.ERROR
import eu.kanade.tachiyomi.source.model.Page.State.LOAD_PAGE
import eu.kanade.tachiyomi.source.model.Page.State.QUEUE
import eu.kanade.tachiyomi.source.model.Page.State.READY
import eu.kanade.tachiyomi.ui.reader.PageIndicatorText
import eu.kanade.tachiyomi.ui.reader.ReaderAppBars
import eu.kanade.tachiyomi.ui.reader.ReaderPageSheetMeta
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.CombinedCircularProgressIndicator
import kotlinx.coroutines.launch
import moe.tarsin.kt.unreachable

@Destination
@Composable
fun ReaderScreen(info: BaseGalleryInfo, page: Int = -1) {
    val scope = rememberCoroutineScope()
    val dialogState = LocalDialogState.current
    LockDrawer(true)
    val cropBorder by Settings.cropBorder.collectAsState()
    val pageLoader = remember(cropBorder) {
        val archive = DownloadManager.getDownloadInfo(info.gid)?.archiveFile
        archive?.let { ArchivePageLoader(it, info.gid, page) } ?: EhPageLoader(info, page)
    }
    DisposableEffect(pageLoader) {
        pageLoader.start()
        onDispose {
            pageLoader.stop()
        }
    }
    val showSeekbar by Settings.showReaderSeekbar.collectAsState()
    val readingMode by Settings.readingMode.collectAsState { ReadingModeType.fromPreference(it) }
    val defaultError = stringResource(id = R.string.decode_image_error)
    Deferred({ pageLoader.awaitReady() }) {
        val lazyListState = rememberLazyListState()
        val pagerState = rememberPagerState { pageLoader.size }
        val syncState = rememberSliderPagerDoubleSyncState(lazyListState, pagerState, pageLoader)
        syncState.Sync()
        Box {
            var appbarVisible by remember { mutableStateOf(false) }
            ReaderAppBars(
                visible = appbarVisible,
                isRtl = readingMode == ReadingModeType.RIGHT_TO_LEFT,
                showSeekBar = showSeekbar,
                currentPage = syncState.sliderValue,
                totalPages = pageLoader.size,
                onSliderValueChange = { syncState.sliderScrollTo(it + 1) },
                onClickSettings = { },
                modifier = Modifier.zIndex(1f),
            )
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodySmall) {
                PageIndicatorText(
                    currentPage = syncState.sliderValue,
                    totalPages = pageLoader.size,
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().zIndex(0.5f),
                )
            }
            GalleryPager(
                type = readingMode,
                pagerState = pagerState,
                lazyListState = lazyListState,
                pageLoader = pageLoader,
                onSelectPage = { page ->
                    scope.launch {
                        dialogState.dialog {
                            ModalBottomSheet(
                                onDismissRequest = { it.cancel() },
                                windowInsets = WindowInsets(0),
                            ) {
                                ReaderPageSheetMeta(
                                    retry = { pageLoader.retryPage(page.index) },
                                    retryOrigin = { pageLoader.retryPage(page.index, true) },
                                    share = {},
                                    copy = {},
                                    save = {},
                                    saveTo = {},
                                    dismiss = { it.cancel() },
                                )
                                Spacer(modifier = Modifier.navigationBarsPadding())
                            }
                        }
                    }
                },
                onMenuRegionClick = { appbarVisible = !appbarVisible },
                item = { page ->
                    val state by page.status.collectAsState()
                    when (state) {
                        QUEUE, LOAD_PAGE, DOWNLOAD_IMAGE -> {
                            LaunchedEffect(Unit) {
                                pageLoader.request(page.index)
                            }
                            Box(
                                modifier = Modifier.fillMaxWidth().aspectRatio(DEFAULT_ASPECT),
                                contentAlignment = Alignment.Center,
                            ) {
                                val progress by page.progressFlow.collectAsState()
                                CombinedCircularProgressIndicator(progress = progress / 100f)
                            }
                        }
                        READY -> {
                            val image = page.image!!.innerImage!!
                            val rect = page.image!!.rect
                            val painter = remember(image) { image.toPainter(rect) }
                            Image(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = if (ReadingModeType.allowHeightOverScreen(readingMode)) ContentScale.FillWidth else ContentScale.Fit,
                            )
                        }
                        ERROR -> {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(DEFAULT_ASPECT)) {
                                Column(
                                    modifier = Modifier.align(Companion.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = page.errorMsg ?: defaultError,
                                        textAlign = TextAlign.Center,
                                    )
                                    Button(onClick = { pageLoader.retryPage(page.index) }) {
                                        Text(text = stringResource(id = R.string.action_retry))
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun CoilImage.toPainter(rect: Rect) = when (this) {
    is BitmapImage -> BitmapPainter(
        image = bitmap.asImageBitmap(),
        srcOffset = IntOffset(rect.left, rect.top),
        srcSize = IntSize(rect.width(), rect.height()),
    )
    is DrawableImage -> DrawablePainter(drawable)
    else -> unreachable()
}

private const val DEFAULT_ASPECT = 1 / 1.4125f
