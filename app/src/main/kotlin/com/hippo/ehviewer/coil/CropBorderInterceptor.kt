package com.hippo.ehviewer.coil

import android.graphics.Bitmap
import androidx.compose.ui.unit.IntRect
import coil3.BitmapImage
import coil3.Extras
import coil3.Image
import coil3.asCoilImage
import coil3.getExtra
import coil3.intercept.Interceptor
import coil3.intercept.Interceptor.Chain
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.hippo.ehviewer.image.detectBorder
import com.hippo.ehviewer.util.isAtLeastO

private const val CROP_THRESHOLD = 0.75f
private const val WEBTOON_THRESHOLD = 3

private val maybeCropBorderKey = Extras.Key(default = false)

fun ImageRequest.Builder.maybeCropBorder(enable: Boolean) = apply {
    extras[maybeCropBorderKey] = enable
}

val ImageRequest.maybeCropBorder: Boolean
    get() = getExtra(maybeCropBorderKey)

class BitmapImageWithRect(
    val rect: IntRect,
    val image: Image,
) : Image by image

object CropBorderInterceptor : Interceptor {
    override suspend fun intercept(chain: Chain): ImageResult {
        val req = chain.request
        if (req.maybeCropBorder) {
            val hw = req.allowHardware && isAtLeastO
            val result = if (hw) {
                val nonHw = req.newBuilder().allowHardware(false).build()
                chain.withRequest(nonHw).proceed()
            } else {
                chain.proceed()
            }
            if (result is SuccessResult) {
                val image = result.image
                if (image is BitmapImage) {
                    val bitmap = image.bitmap
                    val rect = if (image.height / image.width < WEBTOON_THRESHOLD) {
                        val array = detectBorder(bitmap)
                        val minWidth = image.width * CROP_THRESHOLD
                        val minHeight = image.height * CROP_THRESHOLD
                        IntRect(array[0], array[1], array[2], array[3]).takeIf {
                            it.width > minWidth && it.height > minHeight
                        }
                    } else {
                        null
                    }
                    val maybeHwImage = if (hw) {
                        // Upload to Graphical Buffer to accelerate render
                        // May fail if the image is too long
                        bitmap.copy(Bitmap.Config.HARDWARE, false)?.run {
                            bitmap.recycle()
                            asCoilImage()
                        } ?: image
                    } else {
                        image
                    }
                    val new = if (rect != null) {
                        BitmapImageWithRect(rect, maybeHwImage)
                    } else {
                        maybeHwImage
                    }
                    return result.copy(image = new, request = result.request, dataSource = result.dataSource)
                }
            }
            return result
        }
        return chain.proceed()
    }
}
