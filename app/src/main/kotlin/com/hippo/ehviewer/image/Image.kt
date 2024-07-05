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
import android.graphics.drawable.Animatable
import androidx.compose.ui.unit.IntRect
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import coil3.BitmapImage
import coil3.DrawableImage
import coil3.Image as CoilImage
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Dimension
import coil3.size.Precision
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.coil.BitmapImageWithRect
import com.hippo.ehviewer.coil.hardwareThreshold
import com.hippo.ehviewer.coil.maybeCropBorder
import com.hippo.ehviewer.jni.isGif
import com.hippo.ehviewer.jni.mmap
import com.hippo.ehviewer.jni.munmap
import com.hippo.ehviewer.jni.rewriteGifSource
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.util.isAtLeastP
import com.hippo.ehviewer.util.isAtLeastU
import com.hippo.files.openFileDescriptor
import com.hippo.files.toUri
import eu.kanade.tachiyomi.util.system.logcat
import java.nio.ByteBuffer
import okio.Path
import splitties.init.appCtx

class Image private constructor(image: CoilImage, private val src: AutoCloseable) {
    val size = image.size
    val rect = when (image) {
        is BitmapImageWithRect -> image.rect
        else -> image.run { IntRect(0, 0, width, height) }
    }

    var innerImage: CoilImage? = when (image) {
        is BitmapImageWithRect -> image.image
        else -> image
    }

    @Synchronized
    fun recycle() {
        when (val image = innerImage ?: return) {
            is DrawableImage -> {
                (image.drawable as Animatable).stop()
                image.drawable.callback = null
                src.close()
            }
            is BitmapImage -> image.bitmap.recycle()
        }
        innerImage = null
    }

    companion object {
        private val targetWidth = appCtx.resources.displayMetrics.widthPixels * 3

        private suspend fun Either<ByteBufferSource, PathSource>.decodeCoil(): CoilImage {
            val req = appCtx.imageRequest {
                onLeft { data(it.source) }
                onRight { data(it.source.toUri()) }
                size(Dimension(targetWidth), Dimension.Undefined)
                precision(Precision.INEXACT)
                allowHardware(false)
                hardwareThreshold(Settings.hardwareBitmapThreshold)
                maybeCropBorder(Settings.cropBorder.value)
                memoryCachePolicy(CachePolicy.DISABLED)
            }
            return when (val result = appCtx.imageLoader.execute(req)) {
                is SuccessResult -> result.image
                is ErrorResult -> throw result.throwable
            }
        }

        suspend fun decode(src: ImageSource): Image? {
            return runCatching {
                val image = when (src) {
                    is PathSource -> {
                        if (isAtLeastP && !isAtLeastU) {
                            src.source.openFileDescriptor("rw").use {
                                val fd = it.fd
                                if (isGif(fd)) {
                                    val buffer = mmap(fd)!!
                                    val source = object : ByteBufferSource {
                                        override val source = buffer
                                        override fun close() {
                                            munmap(buffer)
                                            src.close()
                                        }
                                    }
                                    return decode(source)
                                }
                            }
                        }
                        src.right().decodeCoil()
                    }

                    is ByteBufferSource -> {
                        if (isAtLeastP && !isAtLeastU) {
                            rewriteGifSource(src.source)
                        }
                        src.left().decodeCoil()
                    }
                }
                Image(image, src).apply {
                    if (innerImage is BitmapImage) src.close()
                }
            }.onFailure {
                src.close()
                logcat(it)
            }.getOrNull()
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

external fun detectBorder(bitmap: Bitmap): IntArray
