package com.hippo.ehviewer.coil

import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.isV2PreviewKey
import moe.tarsin.coroutines.NamedMutex
import moe.tarsin.coroutines.withLockNeedSuspend

object MergeInterceptor : Interceptor {
    private val mutex = NamedMutex<String>()

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val req = chain.request
        val key = req.memoryCacheKey?.takeIf { it.isV2PreviewKey || Settings.preloadThumbAggressively }
        return if (key != null) {
            val (result, suspended) = mutex.withLockNeedSuspend(key) { chain.proceed() }
            when (result) {
                is SuccessResult if (suspended) -> result.copy(dataSource = DataSource.MEMORY)
                else -> result
            }
        } else {
            chain.proceed()
        }
    }
}
