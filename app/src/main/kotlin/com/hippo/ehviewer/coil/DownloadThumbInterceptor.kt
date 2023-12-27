package com.hippo.ehviewer.coil

import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.hippo.ehviewer.EhApplication.Companion.imageCache
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.spider.DownloadInfoMagics.decodeMagicRequestOrUrl
import com.hippo.ehviewer.util.sendTo
import com.hippo.unifile.asUniFile
import eu.kanade.tachiyomi.util.lang.withIOContext

object DownloadThumbInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val magicOrUrl = chain.request.data as? String
        if (magicOrUrl != null) {
            val (url, location) = decodeMagicRequestOrUrl(magicOrUrl)
            if (location != null) {
                val thumb = withIOContext { downloadLocation / location / ".thumb" }
                if (withIOContext { thumb.isFile }) {
                    val new = chain.request.newBuilder().data(thumb.uri.toString()).build()
                    when (val result = chain.withRequest(new).proceed()) {
                        is SuccessResult -> return result
                        is ErrorResult -> withIOContext { thumb.delete() }
                    }
                }
                val new = chain.request.newBuilder().data(url).build()
                return chain.withRequest(new).proceed().also {
                    if (it is SuccessResult) {
                        withIOContext {
                            if (thumb.ensureFile()) {
                                val key = requireNotNull(chain.request.memoryCacheKey)
                                imageCache.read(key) {
                                    data.asUniFile() sendTo thumb
                                }
                            }
                        }
                    }
                }
            }
        }
        return chain.proceed()
    }
}
