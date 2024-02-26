package com.hippo.ehviewer.coil

import coil3.intercept.Interceptor
import coil3.intercept.Interceptor.Chain
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.allowHardware

/**
 * Allocating hardware bitmaps may fail on extremely long images.
 *
 * Retry with [allowHardware] disabled.
 */
object HardwareBitmapInterceptor : Interceptor {
    override suspend fun intercept(chain: Chain): ImageResult {
        val result = chain.proceed()
        return if (chain.request.allowHardware && result is ErrorResult && result.throwable is OutOfMemoryError) {
            val req = chain.request.newBuilder().allowHardware(false).build()
            chain.withRequest(req).proceed()
        } else {
            result
        }
    }
}
