package com.hippo.ehviewer.coil

import coil3.Extras
import coil3.getExtra
import coil3.intercept.Interceptor
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.hippo.ehviewer.EhApplication.Companion.imageCache
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.client.getThumbKey
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.util.sendTo
import com.hippo.files.delete
import com.hippo.files.isDirectory
import com.hippo.files.isFile
import com.hippo.files.toUri

private val downloadInfoKey = Extras.Key<DownloadInfo?>(default = null)

fun ImageRequest.Builder.downloadInfo(info: DownloadInfo) = apply {
    extras[downloadInfoKey] = info
}

val ImageRequest.downloadInfo: DownloadInfo?
    get() = getExtra(downloadInfoKey)

object DownloadThumbInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val info = chain.request.downloadInfo
        if (info != null && !info.dirname.isNullOrBlank()) {
            val thumbKey = getThumbKey(chain.request.data as String)
            if (info.thumbKey != thumbKey) {
                info.thumbKey = thumbKey
                EhDB.putGalleryInfo(info.galleryInfo)
            }
            val dir = downloadLocation / info.dirname
            val format = thumbKey.substringAfterLast('.', "")
            check(format.isNotBlank())
            val thumb = dir / "thumb.$format"
            val v1Thumb = dir / "thumb.jpg"
            if (thumb.isFile) {
                val new = chain.request.newBuilder().data(thumb.toUri()).build()
                val result = chain.withRequest(new).proceed()
                if (result is SuccessResult) {
                    if (thumb != v1Thumb) v1Thumb.delete()
                    return result
                }
            }
            val result = chain.proceed()
            if (result is SuccessResult && dir.isDirectory) {
                // Accessing the recreated file immediately after deleting it throws
                // FileNotFoundException, so we just overwrite the existing file.
                val key = requireNotNull(chain.request.memoryCacheKey)
                imageCache.read(key) {
                    data sendTo thumb
                }
                if (thumb != v1Thumb) v1Thumb.delete()
            }
            return result
        }
        return chain.proceed()
    }
}
