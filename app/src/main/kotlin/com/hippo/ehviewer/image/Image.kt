/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package com.hippo.ehviewer.image

import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import androidx.compose.ui.unit.IntSize
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.fx.coroutines.ExitCase
import arrow.fx.coroutines.bracketCase
import coil3.BitmapImage
import coil3.DrawableImage
import coil3.Image as CoilImage
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Dimension
import coil3.size.Precision
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.coil.BitmapImageWithExtraInfo
import com.hippo.ehviewer.coil.detectQrCode
import com.hippo.ehviewer.coil.hardwareThreshold
import com.hippo.ehviewer.coil.maybeCropBorder
import com.hippo.ehviewer.jni.isGif
import com.hippo.ehviewer.jni.mmap
import com.hippo.ehviewer.jni.munmap
import com.hippo.ehviewer.jni.rewriteGifSource
import com.hippo.ehviewer.ktbuilder.execute
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.util.isAtLeastP
import com.hippo.ehviewer.util.isAtLeastU
import com.hippo.ehviewer.util.updateAndGet
import com.hippo.files.openFileDescriptor
import com.hippo.files.toUri
import java.nio.ByteBuffer
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.decrementAndFetch
import okio.Path
import splitties.init.appCtx

class Image private constructor(image: CoilImage, private val src: ImageSource) {
    val refcnt = AtomicInt(1)

    fun pin() = refcnt.updateAndGet { if (it != 0) it + 1 else 0 } != 0

    fun unpin() = (refcnt.decrementAndFetch() == 0).also { if (it) recycle() }

    val intrinsicSize = with(image) { IntSize(width, height) }
    val allocationSize = image.size
    val hasQrCode = when (image) {
        is BitmapImageWithExtraInfo -> image.hasQrCode
        else -> false
    }

    var innerImage: CoilImage? = when (image) {
        is BitmapImageWithExtraInfo -> image.image
        else -> image
    }

    private fun recycle() {
        when (val image = innerImage!!) {
            is DrawableImage -> src.close()
            is BitmapImage -> image.bitmap.recycle()
        }
        innerImage = null
    }

    companion object {
        private val targetWidth = appCtx.resources.displayMetrics.widthPixels * 3

        private suspend fun Either<ByteBufferSource, PathSource>.decodeCoil(checkExtraneousAds: Boolean): CoilImage {
            val request = appCtx.imageRequest {
                onLeft { data(it.source) }
                onRight { data(it.source.toUri()) }
                size(Dimension(targetWidth), Dimension.Undefined)
                precision(Precision.INEXACT)
                allowHardware(false)
                hardwareThreshold(Settings.hardwareBitmapThreshold)
                maybeCropBorder(Settings.cropBorder.value)
                detectQrCode(checkExtraneousAds)
                memoryCachePolicy(CachePolicy.DISABLED)
            }
            return when (val result = request.execute()) {
                is SuccessResult -> result.image
                is ErrorResult -> throw result.throwable
            }
        }

        suspend fun decode(src: ImageSource, checkExtraneousAds: Boolean = false): Image {
            val image = when (src) {
                is PathSource -> {
                    if (isAtLeastP && !isAtLeastU) {
                        src.source.openFileDescriptor("rw").use {
                            val fd = it.fd
                            if (isGif(fd)) {
                                return bracketCase(
                                    { mmap(fd)!! },
                                    { buffer -> decode(byteBufferSource(buffer) { munmap(buffer).also { src.close() } }, checkExtraneousAds) },
                                    { buffer, case -> if (case !is ExitCase.Completed) munmap(buffer) },
                                )
                            }
                        }
                    }
                    src.right().decodeCoil(checkExtraneousAds)
                }

                is ByteBufferSource -> {
                    if (isAtLeastP && !isAtLeastU) {
                        rewriteGifSource(src.source)
                    }
                    src.left().decodeCoil(checkExtraneousAds)
                }
            }
            return Image(image, src).apply {
                if (innerImage is BitmapImage) src.close()
            }
        }
    }
}

sealed interface ImageSource : AutoCloseable

interface PathSource : ImageSource {
    val source: Path
    val type: String
}

interface ByteBufferSource : ImageSource {
    val source: ByteBuffer
}

inline fun byteBufferSource(buffer: ByteBuffer, crossinline release: () -> Unit) = object : ByteBufferSource {
    override val source = buffer
    override fun close() = release()
}

external fun detectBorder(bitmap: Bitmap): IntArray
external fun hasQrCode(bitmap: Bitmap): Boolean
external fun copyBitmapToAHB(src: Bitmap, dst: HardwareBuffer, x: Int, y: Int)
