package com.hippo.ehviewer.coil

import androidx.compose.ui.unit.IntRect
import coil3.Extras
import coil3.getExtra
import coil3.intercept.Interceptor
import coil3.intercept.Interceptor.Chain
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.hippo.ehviewer.image.detectBorder
import com.hippo.ehviewer.image.detectBorderRust
import kotlin.system.measureNanoTime

private const val CROP_THRESHOLD = 0.75f
private const val RATIO_THRESHOLD = 2

private val maybeCropBorderKey = Extras.Key(default = false)

fun ImageRequest.Builder.maybeCropBorder(enable: Boolean) = apply {
    extras[maybeCropBorderKey] = enable
}

val ImageRequest.maybeCropBorder: Boolean
    get() = getExtra(maybeCropBorderKey)

object CropBorderInterceptor : Interceptor {
    override suspend fun intercept(chain: Chain): ImageResult {
        val result = chain.proceed()
        if (chain.request.maybeCropBorder && result is SuccessResult) {
            val image = result.image
            if (image is BitmapImageWithExtraInfo && !image.hasQrCode) {
                val bitmap = image.image.bitmap
                val ratio = if (image.height > image.width) {
                    image.height / image.width
                } else {
                    image.width / image.height
                }
                if (ratio < RATIO_THRESHOLD) {
                    val arrayRust: IntArray
                    measureNanoTime {
                        arrayRust = detectBorderRust(bitmap)
                    }.let {
                        println("Rust $it ns")
                    }
                    val (a, b, c, d) = arrayRust
                    val arrayC: IntArray
                    measureNanoTime {
                        arrayC = detectBorder(bitmap)
                    }.let {
                        println("C $it ns")
                    }
                    val (e, f, g, h) = arrayC
                    println("$a $b $c $d Rust")
                    println("$e $f $g $h C")
                    val minWidth = image.width * CROP_THRESHOLD
                    val minHeight = image.height * CROP_THRESHOLD
                    val array = arrayRust
                    val rect = IntRect(array[0], array[1], array[2], array[3])
                    if (rect.width > minWidth && rect.height > minHeight) {
                        val new = image.copy(rect = rect)
                        return result.copy(image = new)
                    }
                }
            }
        }
        return result
    }
}
