package com.hippo.ehviewer.coil

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.isNormalPreviewKey
import moe.tarsin.coroutines.NamedMutex
import moe.tarsin.coroutines.withLock

object MergeInterceptor : Interceptor {
    private val mutex = NamedMutex<String>(capacity = 24)

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val req = chain.request
        val key = req.memoryCacheKey?.takeIf { it.isNormalPreviewKey || Settings.preloadThumbAggressively }
        return if (key != null) {
            mutex.withLock(key) { chain.proceed() }
        } else {
            chain.proceed()
        }
    }
}
