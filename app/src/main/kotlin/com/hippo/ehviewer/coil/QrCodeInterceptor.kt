package com.hippo.ehviewer.coil

import androidx.collection.SieveCache
import coil3.Extras
import coil3.Image
import coil3.getExtra
import coil3.intercept.Interceptor
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.image.hasQrCode
import eu.kanade.tachiyomi.util.system.logcat
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import moe.tarsin.coroutines.runSuspendCatching

private val detectQrCodeKey = Extras.Key(default = false)

fun ImageRequest.Builder.detectQrCode(enable: Boolean) = apply {
    extras[detectQrCodeKey] = enable
}

private val cache = SieveCache<MemoryCache.Key, Boolean>(50)
private val lock = ReentrantReadWriteLock()

val ImageRequest.detectQrCode: Boolean
    get() = getExtra(detectQrCodeKey)

object QrCodeInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val result = chain.proceed()
        if (chain.request.detectQrCode && result is SuccessResult) {
            val image = result.image
            if (image is BitmapImageWithExtraInfo) {
                fun compute() = runSuspendCatching { hasQrCode(image.image.bitmap) }.onFailure { logcat(it) }.getOrThrow()
                val hasQrCode = when (val key = result.memoryCacheKey) {
                    is MemoryCache.Key -> lock.read { cache[key] } ?: compute().also { lock.write { cache[key] = it } }
                    null -> compute()
                }
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

fun detectAds(index: Int, size: Int, enable: Boolean = true) = index > size - 10 && Settings.stripExtraneousAds.value && enable
