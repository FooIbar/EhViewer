package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntRect
import coil3.BitmapImage
import coil3.DrawableImage
import coil3.Image
import com.google.accompanist.drawablepainter.DrawablePainter
import com.hippo.ehviewer.R
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.CombinedCircularProgressIndicator
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.ZoomableState
import moe.tarsin.kt.unreachable

@Composable
fun PagerItem(
    page: ReaderPage,
    pageLoader: PageLoader,
    zoomableState: ZoomableState,
    webtoon: Boolean,
) {
    val defaultError = stringResource(id = R.string.decode_image_error)
    val state by page.status.collectAsState()
    when (state) {
        Page.State.QUEUE, Page.State.LOAD_PAGE, Page.State.DOWNLOAD_IMAGE -> {
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
        Page.State.READY -> {
            val image = page.image!!.innerImage!!
            val rect = page.image!!.rect
            val painter = remember(image) { image.toPainter(rect) }
            val contentScale = if (webtoon) ContentScale.FillWidth else ContentScale.Inside
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
            if (!webtoon) {
                val size = painter.intrinsicSize
                LaunchedEffect(size) {
                    val contentLocation = ZoomableContentLocation.scaledInsideAndCenterAligned(size)
                    zoomableState.setContentLocation(contentLocation)
                }
            }
        }
        Page.State.ERROR -> {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(DEFAULT_ASPECT)) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
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
}

private fun Image.toPainter(rect: IntRect) = when (this) {
    is BitmapImage -> BitmapPainter(
        image = bitmap.asImageBitmap(),
        srcOffset = rect.topLeft,
        srcSize = rect.size,
    )
    is DrawableImage -> DrawablePainter(drawable)
    else -> unreachable()
}

private const val DEFAULT_ASPECT = 1 / 1.4125f
