package com.hippo.ehviewer.coil

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import coil3.BitmapImage
import coil3.asCoilImage
import coil3.intercept.Interceptor
import coil3.intercept.Interceptor.Chain
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.allowHardware

@RequiresApi(Build.VERSION_CODES.O)
object HardwareBitmapInterceptor : Interceptor {
    override suspend fun intercept(chain: Chain): ImageResult {
        val result = chain.proceed()
        if (!chain.request.allowHardware && result is SuccessResult) {
            val image = when (val image = result.image) {
                is BitmapImageWithRect -> image.image
                is BitmapImage -> image
                else -> return result
            }
            val bitmap = image.bitmap
            // Large hardware bitmaps have rendering issues (e.g. crash, empty) on some devices.
            // This is not ideal but I haven't figured out how to probe the threshold.
            // All we know is that it's less than the maximum texture size.
            if (maxOf(bitmap.width, bitmap.height) <= 8192) {
                bitmap.copy(Bitmap.Config.HARDWARE, false)?.let {
                    bitmap.recycle()
                    return result.copy(image = it.asCoilImage(), request = result.request, dataSource = result.dataSource)
                }
            }
        }
        return result
    }
}
