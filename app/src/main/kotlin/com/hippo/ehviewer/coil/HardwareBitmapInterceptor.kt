package com.hippo.ehviewer.coil

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.IntRect
import coil3.BitmapImage
import coil3.Extras
import coil3.asImage
import coil3.getExtra
import coil3.intercept.Interceptor
import coil3.intercept.Interceptor.Chain
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.allowHardware

private val hardwareThresholdKey = Extras.Key(default = 16384)

fun ImageRequest.Builder.hardwareThreshold(size: Int) = apply {
    extras[hardwareThresholdKey] = size
}

val ImageRequest.hardwareThreshold: Int
    get() = getExtra(hardwareThresholdKey)

@RequiresApi(Build.VERSION_CODES.O)
object HardwareBitmapInterceptor : Interceptor {
    override suspend fun intercept(chain: Chain): ImageResult {
        val result = chain.proceed()
        val request = result.request
        if (!request.allowHardware && result is SuccessResult) {
            var rect: IntRect? = null
            val image = result.image
            val bitmap = when (image) {
                is BitmapImageWithRect -> image.image.also { rect = image.rect }
                is BitmapImage -> image
                else -> return result
            }.bitmap
            // Large hardware bitmaps have rendering issues (e.g. crash, empty) on some devices.
            // This is not ideal but I haven't figured out how to probe the threshold.
            // All we know is that it's less than the maximum texture size.
            if (maxOf(bitmap.width, bitmap.height) <= request.hardwareThreshold) {
                bitmap.copy(Bitmap.Config.HARDWARE, false)?.let { hwBitmap ->
                    bitmap.recycle()
                    val newImage = hwBitmap.asImage()
                    return result.copy(
                        image = rect?.let { BitmapImageWithRect(it, newImage as BitmapImage) } ?: newImage,
                        request = request,
                        dataSource = result.dataSource,
                    )
                }
            }
        }
        return result
    }
}
