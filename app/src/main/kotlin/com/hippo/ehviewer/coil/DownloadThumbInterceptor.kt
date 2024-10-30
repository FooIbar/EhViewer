package com.hippo.ehviewer.coil

import coil3.Extras
import coil3.getExtra
import coil3.intercept.Interceptor
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.hippo.ehviewer.EhApplication.Companion.imageCache
import com.hippo.ehviewer.util.sendTo
import com.hippo.files.isDirectory
import com.hippo.files.isFile
import com.hippo.files.toUri
import okio.Path
import okio.Path.Companion.toPath

private val emptyPath = "".toPath()
private val downloadLocationKey = Extras.Key(default = emptyPath)

fun ImageRequest.Builder.downloadLocation(path: Path) = apply {
    extras[downloadLocationKey] = path
}

val ImageRequest.downloadLocation: Path
    get() = getExtra(downloadLocationKey)

object DownloadThumbInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val thumb = chain.request.downloadLocation
        if (thumb != emptyPath) {
            if (thumb.isFile) {
                val new = chain.request.newBuilder().data(thumb.toUri()).build()
                val result = chain.withRequest(new).proceed()
                if (result is SuccessResult) return result
            }
            val new = chain.request.newBuilder().build()
            val result = chain.withRequest(new).proceed()
            if (result is SuccessResult && thumb.parent?.isDirectory == true) {
                // Accessing the recreated file immediately after deleting it throws
                // FileNotFoundException, so we just overwrite the existing file.
                val key = requireNotNull(chain.request.memoryCacheKey)
                imageCache.read(key) {
                    data sendTo thumb
                }
            }
            return result
        }
        return chain.proceed()
    }
}
