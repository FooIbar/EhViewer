package com.hippo.ehviewer.ui.main

import androidx.compose.animation.SharedTransitionScope
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
import coil3.Image
import coil3.compose.AsyncImage
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
    return remember(model) { context.imageRequest(model) }
}

context(SharedTransitionScope, TransitionsVisibilityScope, SETNodeGenerator)
@Composable
@NonRestartableComposable
fun EhAsyncThumb(
    model: GalleryInfo,
    modifier: Modifier = Modifier,
    onSuccess: ((Image) -> Unit)? = null,
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
