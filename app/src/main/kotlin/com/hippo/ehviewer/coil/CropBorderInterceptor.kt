package com.hippo.ehviewer.coil

import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntRect
import arrow.fx.coroutines.autoCloseable
import arrow.fx.coroutines.resourceScope
import coil3.asImage
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.colorSpace
import com.hippo.ehviewer.image.copyBitmapToAHBWithRect
import com.hippo.ehviewer.util.isAtLeastQ
import moe.tarsin.coroutines.runSuspendCatching

private const val FORMAT = HardwareBuffer.RGBA_8888
private const val USAGE = HardwareBuffer.USAGE_CPU_READ_RARELY or HardwareBuffer.USAGE_CPU_WRITE_RARELY or HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE

object CropBorderInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val result = chain.proceed()
        if (result is SuccessResult) {
            val image = result.image
            if (image is BitmapImageWithExtraInfo) {
                // Copy with cropped region
                val srcSize = IntSize(image.width, image.height)
                if (image.rect.size != srcSize) {
                    val (x, y) = image.rect.topLeft
                    val (w, h) = image.rect.size
                    val src = image.image.bitmap

                    // Large hardware bitmaps have rendering issues (e.g. crash, empty) on some devices.
                    // This is not ideal but I haven't figured out how to probe the threshold.
                    // All we know is that it's less than the maximum texture size.
                    if (isAtLeastQ && maxOf(w, h) <= chain.request.hardwareThreshold) {
                        runSuspendCatching {
                            resourceScope {
                                val buffer = autoCloseable { HardwareBuffer.create(w, h, FORMAT, 1, USAGE) }
                                copyBitmapToAHBWithRect(src, buffer, x, y, w, h)
                                Bitmap.wrapHardwareBuffer(buffer, chain.request.colorSpace)
                            }
                        }.onSuccess { bitmap ->
                            bitmap?.let {
                                return result.copy(image = image.copy(image = bitmap.asImage(), rect = image.rect.size.toIntRect()))
                            }
                        }
                    }

                    Bitmap.createBitmap(src, x, y, w, h).also { cropped ->
                        src.recycle()
                        return result.copy(image = image.copy(image = cropped.asImage(), rect = image.rect.size.toIntRect()))
                    }
                }
            }
        }
        return result
    }
}
