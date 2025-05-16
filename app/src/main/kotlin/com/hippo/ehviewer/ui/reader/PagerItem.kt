package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil3.BitmapImage
import coil3.DrawableImage
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.google.accompanist.drawablepainter.DrawablePainter
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.gallery.Page
import com.hippo.ehviewer.gallery.PageLoader
import com.hippo.ehviewer.gallery.PageStatus
import com.hippo.ehviewer.gallery.progressObserved
import com.hippo.ehviewer.gallery.statusObserved
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.util.AdsPlaceholderFile
import eu.kanade.tachiyomi.ui.reader.viewer.CombinedCircularProgressIndicator
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.drop
import moe.tarsin.kt.unreachable

@Composable
fun PagerItem(
    page: Page,
    pageLoader: PageLoader,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        pageLoader.request(page.index)
        // In case page loader restart
        page.statusFlow.drop(1).collect {
            if (page.statusFlow.value == PageStatus.Queued) {
                pageLoader.request(page.index)
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            pageLoader.cancelRequest(page.index)
        }
    }
    val defaultError = stringResource(id = R.string.decode_image_error)
    when (val state = page.statusObserved) {
        is PageStatus.Queued, is PageStatus.Loading -> {
            Box(
                modifier = modifier.fillMaxWidth().aspectRatio(DEFAULT_ASPECT),
                contentAlignment = Alignment.Center,
            ) {
                CombinedCircularProgressIndicator(progress = state.progressObserved)
            }
        }
        is PageStatus.Ready -> {
            val image = state.image
            var painter by remember { mutableStateOf<Painter?>(null) }
            LaunchedEffect(image) {
                if (image.pin()) {
                    painter = image.toPainter()
                    try {
                        awaitCancellation()
                    } finally {
                        if (image.unpin()) {
                            pageLoader.notifyPageWait(page.index)
                        }
                    }
                }
            }
            painter?.let { painter ->
                val grayScale by Settings.grayScale.collectAsState()
                val invert by Settings.invertedColors.collectAsState()
                Image(
                    // DrawablePainter <: RememberObserver
                    painter = remember(painter) { painter },
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
            } ?: Spacer(modifier = modifier.fillMaxWidth().aspectRatio(DEFAULT_ASPECT))
        }
        is PageStatus.Blocked -> {
            AdsPlaceholder(
                modifier = modifier.fillMaxSize(),
                contentScale = if (contentScale == ContentScale.Inside) ContentScale.Fit else contentScale,
            )
        }
        is PageStatus.Error -> {
            Box(modifier = modifier.fillMaxWidth().aspectRatio(DEFAULT_ASPECT)) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.message ?: defaultError,
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
    is BitmapImage -> BitmapPainter(image.bitmap, intrinsicSize.toSize())
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

@Composable
fun AdsPlaceholder(
    modifier: Modifier = Modifier,
    contentScale: ContentScale,
) = SubcomposeAsyncImage(
    model = AdsPlaceholderFile,
    contentDescription = null,
    modifier = modifier,
    contentScale = contentScale,
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
