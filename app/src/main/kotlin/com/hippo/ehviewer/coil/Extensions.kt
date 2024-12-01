package com.hippo.ehviewer.coil

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.paging.compose.LazyPagingItems
import coil3.decode.BlackholeDecoder
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.data.V2GalleryPreview
import com.hippo.ehviewer.client.thumbUrl
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.ktbuilder.execute

fun ImageRequest.Builder.ehUrl(info: GalleryInfo) = apply {
    val key = info.thumbKey!!
    data(info.thumbUrl)
    val downloadInfo = (info as? DownloadInfo) ?: DownloadManager.getDownloadInfo(info.gid)
    if (downloadInfo != null && !downloadInfo.dirname.isNullOrBlank()) {
        downloadLocation(downloadLocation / downloadInfo.dirname)
    }
    memoryCacheKey(key)
    diskCacheKey(key)
}

fun ImageRequest.Builder.ehPreview(preview: GalleryPreview) = apply {
    with(preview) {
        data(url)
        if (preview is V2GalleryPreview) size(Size.ORIGINAL)
        memoryCacheKey(imageKey)
        diskCacheKey(imageKey)
    }
}

fun ImageRequest.Builder.justDownload() = apply {
    memoryCachePolicy(CachePolicy.DISABLED)
    decoderFactory(BlackholeDecoder.Factory())
}

@Composable
inline fun <T : Any> PrefetchAround(data: LazyPagingItems<T>, index: Int, distance: Int, crossinline f: (T) -> ImageRequest) {
    data.peek((index - distance).coerceAtLeast(0))?.let { fetchBefore ->
        LaunchedEffect(fetchBefore) {
            f(fetchBefore).execute()
        }
    }
    data.peek((index + distance).coerceAtMost(data.itemCount - 1))?.let { fetchAhead ->
        LaunchedEffect(fetchAhead) {
            f(fetchAhead).execute()
        }
    }
}
