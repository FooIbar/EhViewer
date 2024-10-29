package com.hippo.ehviewer.coil

import coil3.decode.BlackholeDecoder
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.data.V2GalleryPreview
import com.hippo.ehviewer.spider.DownloadInfoMagics.encodeMagicRequestOrUrl

fun ImageRequest.Builder.ehUrl(info: GalleryInfo) = apply {
    val key = info.thumbKey!!
    data(encodeMagicRequestOrUrl(info))
    memoryCacheKey(key)
    diskCacheKey(key)
}

fun ImageRequest.Builder.ehPreview(preview: GalleryPreview) = with(preview) {
    data(url)
    if (preview is V2GalleryPreview) size(Size.ORIGINAL)
    memoryCacheKey(imageKey)
    diskCacheKey(imageKey)
}

fun ImageRequest.Builder.justDownload() = apply {
    memoryCachePolicy(CachePolicy.DISABLED)
    decoderFactory(BlackholeDecoder.Factory())
}
