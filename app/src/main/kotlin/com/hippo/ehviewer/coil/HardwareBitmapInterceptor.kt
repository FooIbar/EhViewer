package com.hippo.ehviewer.coil

import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.allowHardware

/**
 * Allocating hardware bitmaps may fail on extremely long images.
 *
 * Retry with [allowHardware] disabled.
 */
object HardwareBitmapInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val result = chain.proceed()
        return if (chain.request.allowHardware && (result as? ErrorResult)?.throwable is OutOfMemoryError) {
            chain.withRequest(chain.request.newBuilder().allowHardware(false).build()).proceed()
        } else {
            result
        }
    }
}
