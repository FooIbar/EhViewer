package com.hippo.ehviewer.coil

import coil3.Extras
import coil3.getExtra
import coil3.intercept.Interceptor
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.hippo.ehviewer.image.hasQrCode

private val detectQrCodeKey = Extras.Key(default = false)

fun ImageRequest.Builder.detectQrCode(enable: Boolean) = apply {
    extras[detectQrCodeKey] = enable
}

val ImageRequest.detectQrCode: Boolean
    get() = getExtra(detectQrCodeKey)

object QrCodeInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val result = chain.proceed()
        if (chain.request.detectQrCode && result is SuccessResult) {
            val image = result.image
            if (image is BitmapImageWithExtraInfo) {
                val hasQrCode = hasQrCode(image.image.bitmap)
                val new = image.copy(hasQrCode = hasQrCode)
                return result.copy(image = new)
            }
        }
        return result
    }
}
