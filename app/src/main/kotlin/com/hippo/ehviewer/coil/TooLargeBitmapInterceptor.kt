package com.hippo.ehviewer.coil

import coil3.intercept.Interceptor
import coil3.intercept.Interceptor.Chain
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.hippo.ehviewer.util.isAtLeastQ

object TooLargeBitmapInterceptor : Interceptor {
    override suspend fun intercept(chain: Chain): ImageResult {
        val isHw = chain.request.allowHardware
        when (val result = chain.proceed()) {
            is SuccessResult -> return result
            is ErrorResult -> {
                val cause = result.throwable
                val canReDecode = isAtLeastQ && isHw && cause is OutOfMemoryError
                if (canReDecode && cause.message == "failed to allocate hardware Bitmap!") {
                    val req = chain.request.newBuilder().allowHardware(false).build()
                    return chain.withRequest(req).proceed()
                }
                return result
            }
        }
    }
}
