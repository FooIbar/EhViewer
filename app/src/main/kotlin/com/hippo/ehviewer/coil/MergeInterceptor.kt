package com.hippo.ehviewer.coil

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.isNormalPreviewKey
import moe.tarsin.coroutines.WeakMutexMap
import moe.tarsin.coroutines.withLock

object MergeInterceptor : Interceptor {
    private val weakMutexMap = WeakMutexMap<String>()

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val key = chain.request.memoryCacheKey
        return if (key != null && (key.isNormalPreviewKey || Settings.preloadThumbAggressively)) {
            weakMutexMap.withLock(key) { chain.proceed() }
        } else {
            chain.proceed()
        }
    }
}
