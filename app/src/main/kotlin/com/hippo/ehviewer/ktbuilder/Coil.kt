package com.hippo.ehviewer.ktbuilder

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.imageLoader
import coil3.request.ImageRequest
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.coil.ehPreview
import com.hippo.ehviewer.coil.ehUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

context(ctx: Context)
inline fun imageRequest(preview: GalleryPreview, builder: ImageRequest.Builder.() -> Unit = {}) = ImageRequest.Builder(ctx).ehPreview(preview).apply(builder).build()
context(ctx: Context)
inline fun imageRequest(info: GalleryInfo, builder: ImageRequest.Builder.() -> Unit = {}) = ImageRequest.Builder(ctx).ehUrl(info).apply(builder).build()
context(ctx: Context)
inline fun imageRequest(builder: ImageRequest.Builder.() -> Unit = {}) = ImageRequest.Builder(ctx).apply(builder).build()
inline fun diskCache(builder: DiskCache.Builder.() -> Unit) = DiskCache.Builder().apply(builder).build()
inline fun Context.imageLoader(builder: ImageLoader.Builder.() -> Unit) = ImageLoader.Builder(this).apply(builder).build()
suspend fun ImageRequest.execute() = context.imageLoader.execute(this)
fun ImageRequest.executeIn(scope: CoroutineScope) = scope.launch { execute() }
