package com.hippo.ehviewer.ui.main

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.Image as CoilImage
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.ConstraintsSizeResolver
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.request.ImageRequest
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.ui.tools.SETNodeGenerator
import com.hippo.ehviewer.ui.tools.TransitionsVisibilityScope
import com.hippo.ehviewer.ui.tools.shouldCrop

@Composable
@NonRestartableComposable
fun requestOf(model: GalleryInfo): ImageRequest {
    val context = LocalContext.current
    return remember(model) { context.imageRequest(model) }.withSizeResolver()
}

@Composable
@NonRestartableComposable
fun ImageRequest.withSizeResolver() = if (defined.sizeResolver != null) {
    this
} else {
    val sizeResolver = rememberConstraintsSizeResolver()
    remember(this, sizeResolver) { newBuilder().size(sizeResolver).build() }
}

fun Modifier.imageRequest(request: ImageRequest): Modifier {
    val sizeResolver = request.sizeResolver
    return if (sizeResolver is ConstraintsSizeResolver) {
        then(sizeResolver)
    } else {
        this
    }
}

context(SharedTransitionScope, TransitionsVisibilityScope, SETNodeGenerator)
@Composable
@NonRestartableComposable
fun EhAsyncThumb(
    model: GalleryInfo,
    modifier: Modifier = Modifier,
    onSuccess: ((CoilImage) -> Unit)? = null,
    contentScale: ContentScale = ContentScale.Fit,
) = AsyncImage(
    model = requestOf(model),
    contentDescription = null,
    modifier = modifier
        // .sharedBounds(key = "${model.gid}")
        .clip(ShapeDefaults.Medium),
    onSuccess = onSuccess?.let { callback ->
        { callback(it.result.image) }
    },
    contentScale = contentScale,
)

context(SharedTransitionScope, TransitionsVisibilityScope, SETNodeGenerator)
@Composable
fun EhAsyncCropThumb(
    key: GalleryInfo,
    modifier: Modifier = Modifier,
) {
    var contentScale by remember(key) { mutableStateOf(ContentScale.Fit) }
    AsyncImage(
        model = requestOf(key),
        contentDescription = null,
        modifier = modifier
            // .sharedBounds(key = "${key.gid}")
            .clip(ShapeDefaults.Medium),
        onSuccess = {
            if (it.result.image.shouldCrop) {
                contentScale = ContentScale.Crop
            }
        },
        contentScale = contentScale,
    )
}

@Composable
fun EhThumbCard(
    key: GalleryInfo,
    modifier: Modifier = Modifier,
) {
    var contentScale by remember(key) { mutableStateOf(ContentScale.Fit) }
    val request = requestOf(key)
    val painter = rememberAsyncImagePainter(
        model = request,
        onSuccess = {
            if (it.result.image.shouldCrop) {
                contentScale = ContentScale.Crop
            }
        },
    )
    Card(
        onClick = {
            if (painter.state.value is AsyncImagePainter.State.Error) {
                painter.restart()
            }
        },
        modifier = modifier,
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.imageRequest(request).fillMaxSize().clip(ShapeDefaults.Medium),
            contentScale = contentScale,
        )
    }
}
