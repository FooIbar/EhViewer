package com.hippo.ehviewer.coil

import android.graphics.Bitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import coil3.asImage
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult

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
                    Bitmap.createBitmap(image.image.bitmap, x, y, w, h).also { cropped ->
                        image.image.bitmap.recycle()
                        return result.copy(image = image.copy(image = cropped.asImage(), rect = IntRect(IntOffset.Zero, image.rect.size)))
                    }
                } else {
                    image.image.bitmap
                }
            }
        }
        return result
    }
}
