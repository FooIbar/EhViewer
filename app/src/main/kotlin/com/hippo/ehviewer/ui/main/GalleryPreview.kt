package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil3.BitmapImage
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.data.NormalGalleryPreview
import com.hippo.ehviewer.coil.hasQrCode
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.ui.reader.AdsPlaceholder
import com.hippo.ehviewer.ui.tools.CrystalCard
import com.hippo.ehviewer.ui.tools.shouldCrop

context(GalleryDetail)
@Composable
@NonRestartableComposable
fun requestOf(model: GalleryPreview): ImageRequest {
    val context = LocalContext.current
    return remember(model) { context.imageRequest(model) }
}

context(GalleryDetail)
@Composable
fun EhAsyncPreview(
    model: GalleryPreview,
    modifier: Modifier = Modifier,
) {
    var contentScale by remember(model) { mutableStateOf(ContentScale.Fit) }
    var blocked by rememberSaveable(model) { mutableStateOf(false) }
    if (blocked) {
        AdsPlaceholder(
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        AsyncImage(
            model = requestOf(model),
            contentDescription = null,
            modifier = modifier,
            transform = {
                with(model) {
                    when (it) {
                        is AsyncImagePainter.State.Success if it.result.image.hasQrCode -> it.also { blocked = true }
                        is AsyncImagePainter.State.Success if this is NormalGalleryPreview -> it.copy(
                            painter = BitmapPainter(
                                (it.result.image as BitmapImage).bitmap.asImageBitmap(),
                                IntOffset(offsetX, 0),
                                IntSize(clipWidth - 1, clipHeight - 1),
                            ),
                        )
                        else -> it
                    }
                }
            },
            onState = {
                if (it is AsyncImagePainter.State.Success) {
                    with(model) {
                        if (this is NormalGalleryPreview) {
                            if (shouldCrop) {
                                contentScale = ContentScale.Crop
                            }
                        } else {
                            if (it.result.image.shouldCrop) {
                                contentScale = ContentScale.Crop
                            }
                        }
                    }
                }
            },
            contentScale = contentScale,
        )
    }
}

context(GalleryDetail)
@Composable
@NonRestartableComposable
fun EhPreviewItem(
    galleryPreview: GalleryPreview?,
    position: Int,
    onClick: () -> Unit,
) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(contentAlignment = Alignment.Center) {
        CrystalCard(
            onClick = onClick,
            modifier = Modifier.aspectRatio(DEFAULT_RATIO),
        ) {
            if (galleryPreview != null) {
                EhAsyncPreview(
                    model = galleryPreview,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
    Text(text = "${position + 1}")
}
