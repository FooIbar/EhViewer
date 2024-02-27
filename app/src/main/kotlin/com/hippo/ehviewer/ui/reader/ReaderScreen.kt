package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection.Rtl
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
import com.ramcosta.composedestinations.annotation.Destination
import eu.kanade.tachiyomi.source.model.Page.State.DOWNLOAD_IMAGE
import eu.kanade.tachiyomi.source.model.Page.State.ERROR
import eu.kanade.tachiyomi.source.model.Page.State.LOAD_PAGE
import eu.kanade.tachiyomi.source.model.Page.State.QUEUE
import eu.kanade.tachiyomi.source.model.Page.State.READY
import eu.kanade.tachiyomi.ui.reader.ReaderAppBars
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import moe.tarsin.kt.unreachable

@Destination
@Composable
fun ReaderScreen(info: BaseGalleryInfo, page: Int = -1) {
    LockDrawer(true)
    val pageLoader = remember {
        val archive = DownloadManager.getDownloadInfo(info.gid)?.archiveFile
        archive?.let { ArchivePageLoader(it, info.gid, page) } ?: EhPageLoader(info, page)
    }
    DisposableEffect(Unit) {
        pageLoader.start()
        onDispose {
            pageLoader.stop()
        }
    }
    val showSeekbar by Settings.showReaderSeekbar.collectAsState()
    val sliderValueFlow = remember { MutableStateFlow(-1) }
    val lazyListState = rememberLazyListState()
    LaunchedEffect(sliderValueFlow) {
        sliderValueFlow.drop(1).debounce(200).collect {
            lazyListState.scrollToItem(it)
        }
    }
    val isRtl = LocalLayoutDirection.current == Rtl
    val currentPage by remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }
    Deferred({ pageLoader.awaitReady() }) {
        Box {
            ReaderAppBars(
                visible = true,
                isRtl = isRtl,
                showSeekBar = showSeekbar,
                currentPage = currentPage,
                totalPages = pageLoader.size,
                onSliderValueChange = { sliderValueFlow.value = it },
                onClickSettings = { },
                modifier = Modifier.zIndex(1f),
            )
            LazyColumn(modifier = Modifier.fillMaxSize(), state = lazyListState) {
                items(pageLoader.mPages) { page ->
                    pageLoader.request(page.index)
                    val state by page.status.collectAsState()
                    when (state) {
                        QUEUE, LOAD_PAGE -> {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(DEFAULT_ASPECT)) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                        DOWNLOAD_IMAGE -> {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(DEFAULT_ASPECT)) {
                                val progress by page.progressFlow.collectAsState()
                                CircularProgressIndicator(
                                    progress = { progress / 100f },
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                        }
                        READY -> {
                            val image = page.image!!.innerImage!!
                            val painter = remember(image) { image.toPainter() }
                            Image(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth,
                            )
                        }
                        ERROR -> {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(DEFAULT_ASPECT)) {
                                Column(
                                    modifier = Modifier.align(Companion.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(text = page.errorMsg.orEmpty())
                                    Button(onClick = { }) {
                                        Text(text = stringResource(id = R.string.action_retry))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun CoilImage.toPainter() = when (this) {
    is BitmapImage -> BitmapPainter(image = bitmap.asImageBitmap())
    is DrawableImage -> DrawablePainter(drawable)
    else -> unreachable()
}

private const val DEFAULT_ASPECT = 1 / 1.4125f
