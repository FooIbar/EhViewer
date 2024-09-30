package com.hippo.ehviewer.coil

import coil3.Extras
import coil3.Image
import coil3.getExtra
import coil3.intercept.Interceptor
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.hippo.ehviewer.image.hasQrCode
import eu.kanade.tachiyomi.util.system.logcat
import moe.tarsin.coroutines.runSuspendCatching

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
                val hasQrCode = runSuspendCatching { hasQrCode(image.image.bitmap) }.onFailure { logcat(it) }.getOrThrow()
                val new = image.copy(hasQrCode = hasQrCode)
                return result.copy(image = new)
            }
        }
        return result
    }
}

val Image.hasQrCode
    get() = when (this) {
        is BitmapImageWithExtraInfo -> hasQrCode
        else -> false
    }
