package com.hippo.ehviewer.coil

import coil3.decode.BlackholeDecoder
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.data.V2GalleryPreview
import com.hippo.ehviewer.client.thumbUrl
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.downloadLocation

fun ImageRequest.Builder.ehUrl(info: GalleryInfo) = apply {
    val key = info.thumbKey!!
    data(info.thumbUrl)
    val format = info.thumbUrl.substringAfterLast('.', "")
    check(format.isNotBlank())
    if (info is DownloadInfo && !info.dirname.isNullOrBlank()) {
        downloadLocation(downloadLocation / info.dirname / "thumb.$format")
    }
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
