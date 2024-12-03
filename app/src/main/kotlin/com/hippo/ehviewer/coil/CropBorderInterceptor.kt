package com.hippo.ehviewer.coil

import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntRect
import arrow.fx.coroutines.autoCloseable
import arrow.fx.coroutines.resourceScope
import coil3.asImage
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.hippo.ehviewer.image.copyBitmapToAHB
import com.hippo.ehviewer.util.isAtLeastQ
import eu.kanade.tachiyomi.util.system.logcat
import moe.tarsin.coroutines.runSuspendCatching

@RequiresApi(Build.VERSION_CODES.O)
private const val USAGE = HardwareBuffer.USAGE_CPU_WRITE_RARELY or HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE

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
                    val meetHardwareThreshold = maxOf(w, h) <= chain.request.hardwareThreshold
                    val bitmap = when {
                        isAtLeastQ && meetHardwareThreshold -> runSuspendCatching {
                            val format = when (val config = src.config) {
                                Bitmap.Config.ARGB_8888 -> HardwareBuffer.RGBA_8888
                                Bitmap.Config.RGB_565 -> HardwareBuffer.RGB_565
                                Bitmap.Config.RGBA_F16 -> HardwareBuffer.RGBA_FP16
                                else -> error("Unsupported bitmap config: $config")
                            }
                            resourceScope {
                                val buffer = autoCloseable { HardwareBuffer.create(w, h, format, 1, USAGE) }
                                copyBitmapToAHB(src, buffer, x, y)
                                Bitmap.wrapHardwareBuffer(buffer, src.colorSpace)
                            }
                        }.onFailure { logcat(it) }.getOrNull()
                        else -> null
                    } ?: Bitmap.createBitmap(src, x, y, w, h)

                    src.recycle()
                    return result.copy(image = image.copy(image = bitmap.asImage(), rect = image.rect.size.toIntRect()))
                }
            }
        }
        return result
    }
}
