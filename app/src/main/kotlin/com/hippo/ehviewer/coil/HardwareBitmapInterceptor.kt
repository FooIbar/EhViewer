package com.hippo.ehviewer.coil

import android.graphics.Bitmap
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import coil3.Extras
import coil3.asImage
import coil3.getExtra
import coil3.intercept.Interceptor
import coil3.intercept.Interceptor.Chain
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.hippo.ehviewer.util.isAtLeastO

private val hardwareThresholdKey = Extras.Key(default = 16384)

fun ImageRequest.Builder.hardwareThreshold(size: Int) = apply {
    extras[hardwareThresholdKey] = size
}

val ImageRequest.hardwareThreshold: Int
    get() = getExtra(hardwareThresholdKey)

object HardwareBitmapInterceptor : Interceptor {
    override suspend fun intercept(chain: Chain): ImageResult {
        val result = chain.proceed()
        val request = result.request
        if (!request.allowHardware && result is SuccessResult) {
            val image = result.image
            if (image is BitmapImageWithExtraInfo) {
                // Copy with cropped region
                val srcSize = IntSize(image.width, image.height)
                val bitmap = if (image.rect.size != srcSize) {
                    val (x, y) = image.rect.topLeft
                    val (w, h) = image.rect.size
                    Bitmap.createBitmap(image.image.bitmap, x, y, w, h).also {
                        image.image.bitmap.recycle()
                    }
                } else {
                    image.image.bitmap
                }

                // Large hardware bitmaps have rendering issues (e.g. crash, empty) on some devices.
                // This is not ideal but I haven't figured out how to probe the threshold.
                // All we know is that it's less than the maximum texture size.
                if (maxOf(bitmap.width, bitmap.height) <= request.hardwareThreshold && isAtLeastO) {
                    bitmap.copy(Bitmap.Config.HARDWARE, false)?.let { hwBitmap ->
                        bitmap.recycle()
                        return result.copy(image = image.copy(image = hwBitmap.asImage(), rect = IntRect.Zero))
                    }
                }

                return result.copy(image = image.copy(image = bitmap.asImage(), rect = IntRect.Zero))
            }
        }
        return result
    }
}
