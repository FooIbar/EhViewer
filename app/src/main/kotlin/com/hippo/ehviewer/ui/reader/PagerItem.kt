package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.BitmapImage
import coil3.DrawableImage
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.google.accompanist.drawablepainter.DrawablePainter
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.ui.settings.AdsPlaceholderFile
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.CombinedCircularProgressIndicator
import kotlinx.coroutines.flow.drop
import moe.tarsin.kt.unreachable

@Composable
fun PagerItem(
    page: ReaderPage,
    pageLoader: PageLoader,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        pageLoader.request(page.index)
        page.status.drop(1).collect {
            if (page.status.value == Page.State.QUEUE) {
                pageLoader.request(page.index)
            }
        }
    }
    val defaultError = stringResource(id = R.string.decode_image_error)
    val state by page.status.collectAsState()
    when (state) {
        Page.State.QUEUE, Page.State.LOAD_PAGE, Page.State.DOWNLOAD_IMAGE -> {
            Box(
                modifier = modifier.fillMaxWidth().aspectRatio(DEFAULT_ASPECT),
                contentAlignment = Alignment.Center,
            ) {
                val progress by page.progressFlow.collectAsState()
                CombinedCircularProgressIndicator(progress = progress / 100f)
            }
        }
        Page.State.READY -> {
            val image = page.image!!
            val painter = remember(image) { image.toPainter() }
            val grayScale by Settings.grayScale.collectAsState()
            val invert by Settings.invertedColors.collectAsState()
            DisposableEffect(image) {
                image.isRecyclable = false
                onDispose {
                    if (image.isRecyclable) {
                        pageLoader.notifyPageWait(page.index)
                        image.recycle()
                    } else {
                        image.isRecyclable = true
                    }
                }
            }
            Image(
                painter = painter,
                contentDescription = null,
                modifier = contentModifier.fillMaxSize(),
                contentScale = contentScale,
                colorFilter = when {
                    grayScale && invert -> grayScaleAndInvertFilter
                    grayScale -> grayScaleFilter
                    invert -> invertFilter
                    else -> null
                },
            )
        }
        Page.State.BLOCKED -> {
            SubcomposeAsyncImage(
                model = AdsPlaceholderFile,
                contentDescription = null,
                modifier = modifier.fillMaxSize(),
                contentScale = if (contentScale == ContentScale.Inside) ContentScale.Fit else contentScale,
            ) {
                val placeholderState by painter.state.collectAsState()
                if (placeholderState is AsyncImagePainter.State.Success) {
                    SubcomposeAsyncImageContent()
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(DEFAULT_ASPECT),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (placeholderState is AsyncImagePainter.State.Error) {
                            Text(text = stringResource(id = R.string.blocked_image))
                        }
                    }
                }
            }
        }
        Page.State.ERROR -> {
            Box(modifier = modifier.fillMaxWidth().aspectRatio(DEFAULT_ASPECT)) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = page.errorMsg ?: defaultError,
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = { pageLoader.retryPage(page.index) }, modifier = Modifier.padding(8.dp)) {
                        Text(text = stringResource(id = R.string.action_retry))
                    }
                }
            }
        }
    }
}

private fun Image.toPainter() = when (val image = innerImage) {
    is BitmapImage -> BitmapPainter(image.bitmap, rect)
    is DrawableImage -> DrawablePainter(image.drawable)
    else -> unreachable()
}

private const val DEFAULT_ASPECT = 1 / 1.4125f

private val invertMatrix = ColorMatrix(
    floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
    ),
)
private val grayScaleMatrix = ColorMatrix().apply { setToSaturation(0f) }
private val grayScaleAndInvertMatrix = ColorMatrix().also { mtx ->
    mtx.setToSaturation(0f)
    mtx *= invertMatrix
}

private val grayScaleFilter = ColorFilter.colorMatrix(grayScaleMatrix)
private val invertFilter = ColorFilter.colorMatrix(invertMatrix)
private val grayScaleAndInvertFilter = ColorFilter.colorMatrix(grayScaleAndInvertMatrix)
