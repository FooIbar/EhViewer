package com.hippo.ehviewer.coil

import coil3.Extras
import coil3.getExtra
import coil3.intercept.Interceptor
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.hippo.ehviewer.image.hasQRCode

private val detectQRCodeKey = Extras.Key(default = false)

fun ImageRequest.Builder.detectQRCode(enable: Boolean) = apply {
    extras[detectQRCodeKey] = enable
}

val ImageRequest.detectQRCode: Boolean
    get() = getExtra(detectQRCodeKey)

object QRCodeInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val result = chain.proceed()
        if (chain.request.detectQRCode && result is SuccessResult) {
            val image = result.image
            if (image is BitmapImageWithExtraInfo) {
                val hasQRCode = hasQRCode(image.image.bitmap)
                val new = image.copy(hasQRCode = hasQRCode)
                return result.copy(image = new)
            }
        }
        return result
    }
}
