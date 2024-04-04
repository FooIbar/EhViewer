package com.hippo.ehviewer.coil

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.hippo.ehviewer.EhApplication.Companion.imageCache
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.spider.DownloadInfoMagics.decodeMagicRequestOrUrl
import com.hippo.ehviewer.util.sendTo
import com.hippo.unifile.asUniFile
import eu.kanade.tachiyomi.util.lang.withIOContext

object DownloadThumbInterceptor : Interceptor {
    // TODO: Remove this after a few releases
    private const val LEGACY_THUMB_FILE = ".thumb"
    private const val THUMB_FILE = "thumb.jpg"
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val magicOrUrl = chain.request.data as? String
        if (magicOrUrl != null) {
            val (url, location) = decodeMagicRequestOrUrl(magicOrUrl)
            if (location != null) {
                val thumb = withIOContext {
                    val legacyThumb = downloadLocation / location / LEGACY_THUMB_FILE
                    if (legacyThumb.isFile) {
                        legacyThumb.apply { renameTo(THUMB_FILE) }
                    } else {
                        downloadLocation / location / THUMB_FILE
                    }
                }
                if (withIOContext { thumb.isFile }) {
                    val new = chain.request.newBuilder().data(thumb.uri.toString()).build()
                    val result = chain.withRequest(new).proceed()
                    if (result is SuccessResult) return result
                }
                val new = chain.request.newBuilder().data(url).build()
                val result = chain.withRequest(new).proceed()
                if (result is SuccessResult) {
                    withIOContext {
                        // Accessing the recreated file immediately after deleting it throws
                        // FileNotFoundException, so we just overwrite the existing file.
                        if (thumb.ensureFile()) {
                            val key = requireNotNull(chain.request.memoryCacheKey)
                            imageCache.read(key) {
                                data.asUniFile() sendTo thumb
                            }
                        }
                    }
                }
                return result
            }
        }
        return chain.proceed()
    }
}
