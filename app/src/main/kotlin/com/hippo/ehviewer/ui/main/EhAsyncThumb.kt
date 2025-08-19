package com.hippo.ehviewer.ui.main

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.ehviewer.core.ui.util.SETNodeGenerator
import com.ehviewer.core.ui.util.SharedElementBox
import com.ehviewer.core.ui.util.TransitionsVisibilityScope
import com.ehviewer.core.ui.util.thenIf
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.ui.tools.shouldCrop

@Composable
@NonRestartableComposable
fun requestOf(model: GalleryInfo) = with(LocalContext.current) {
    remember(model) { imageRequest(model) }
}

@Composable
context(_: SharedTransitionScope, _: TransitionsVisibilityScope, _: SETNodeGenerator)
fun EhAsyncCropThumb(
    key: GalleryInfo,
    modifier: Modifier = Modifier,
) = SharedElementBox(key = "${key.gid}", shape = ShapeDefaults.Medium) {
    var contentScale by remember(key) { mutableStateOf(ContentScale.Fit) }
    val request = requestOf(key)
    Image(
        // https://github.com/coil-kt/coil/issues/2959
        painter = rememberAsyncImagePainter(
            model = request,
            onSuccess = {
                if (it.result.image.shouldCrop) {
                    contentScale = ContentScale.Crop
                }
            },
        ),
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
    )
}

@Composable
context(_: SharedTransitionScope, _: TransitionsVisibilityScope, _: SETNodeGenerator)
fun EhThumbCard(
    key: GalleryInfo,
    modifier: Modifier = Modifier,
) = Card(modifier = modifier) {
    SharedElementBox(key = "${key.gid}", shape = ShapeDefaults.Medium) {
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
        val state by painter.state.collectAsState()
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.thenIf(state !is AsyncImagePainter.State.Success) {
                // Keep applying this when state is `Loading` to avoid cutting off the ripple
                clickable { if (state is AsyncImagePainter.State.Error) painter.restart() }
            }.fillMaxSize(),
            contentScale = contentScale,
        )
    }
}
