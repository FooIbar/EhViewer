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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import coil3.BitmapImage
import coil3.DrawableImage
import coil3.Image as CoilImage
import coil3.asCoilImage
import coil3.executeBlocking
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.colorSpace
import coil3.size.Precision
import coil3.size.Scale
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.jni.isGif
import com.hippo.ehviewer.jni.mmap
import com.hippo.ehviewer.jni.munmap
import com.hippo.ehviewer.jni.rewriteGifSource
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.util.isAtLeastO
import com.hippo.ehviewer.util.isAtLeastP
import com.hippo.ehviewer.util.isAtLeastQ
import com.hippo.ehviewer.util.isAtLeastU
import com.hippo.unifile.UniFile
import java.nio.ByteBuffer
import splitties.init.appCtx

private const val CROP_THRESHOLD = 0.75f

class Image private constructor(image: CoilImage, private val src: AutoCloseable) {
    val size = image.size
    private val intrinsicRect = image.run { Rect(0, 0, width, height) }
    val rect: Rect = if (Settings.cropBorder.value && image is BitmapImage) {
        val array = detectBorder(image.bitmap)
        val r = Rect(array[0], array[1], array[2], array[3])
        val minWidth = image.width * CROP_THRESHOLD
        val minHeight = image.height * CROP_THRESHOLD
        if (r.width() > minWidth && r.height() > minHeight) r else intrinsicRect
    } else {
        intrinsicRect
    }

    var innerImage: CoilImage? = if (image is BitmapImage && isAtLeastQ) {
        val bitmap = image.bitmap

        // Upload to Graphical Buffer to accelerate render
        bitmap.copy(Bitmap.Config.HARDWARE, false).apply {
            bitmap.recycle()
        }.asCoilImage()
    } else {
        image
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
        private val imageSearchMaxSize = appCtx.resources.getDimensionPixelOffset(R.dimen.image_search_max_size)
        private val targetWidth = appCtx.resources.displayMetrics.widthPixels * 2
        private val targetHeight = appCtx.resources.displayMetrics.heightPixels * 2

        @delegate:RequiresApi(Build.VERSION_CODES.O)
        val isWideColorGamut by lazy { appCtx.resources.configuration.isScreenWideColorGamut }

        @RequiresApi(Build.VERSION_CODES.O)
        lateinit var colorSpace: ColorSpace

        private suspend fun Either<ByteBufferSource, UniFileSource>.decodeCoil(): CoilImage {
            val req = appCtx.imageRequest {
                onLeft { data(it.source) }
                onRight { data(it.source.uri) }
                size(targetWidth, targetHeight)
                scale(Scale.FILL)
                precision(Precision.INEXACT)
                if (isAtLeastO) {
                    colorSpace(colorSpace)
                }
                if (Settings.cropBorder.value) {
                    allowHardware(false)
                }
                memoryCachePolicy(CachePolicy.DISABLED)
            }
            return when (val result = appCtx.imageLoader.execute(req)) {
                is SuccessResult -> result.image
                is ErrorResult -> throw result.throwable
            }
        }

        suspend fun decode(src: ImageSource): Image? {
            return runCatching {
                when (src) {
                    is UniFileSource -> {
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
                        val image = src.right().decodeCoil()
                        if (image is BitmapImage) src.close()
                        Image(image, src)
                    }

                    is ByteBufferSource -> {
                        if (isAtLeastP && !isAtLeastU) {
                            rewriteGifSource(src.source)
                        }
                        val image = src.left().decodeCoil()
                        if (image is BitmapImage) src.close()
                        Image(image, src)
                    }
                }
            }.onFailure {
                src.close()
                it.printStackTrace()
            }.getOrNull()
        }

        fun Context.decodeBitmap(uri: Uri): Bitmap {
            val req = imageRequest {
                memoryCachePolicy(CachePolicy.DISABLED)
                data(uri)
                size(imageSearchMaxSize)
                scale(Scale.FILL)
            }
            val image = when (val result = appCtx.imageLoader.executeBlocking(req)) {
                is SuccessResult -> result.image
                is ErrorResult -> throw result.throwable
            }
            return (image as BitmapImage).bitmap
        }
    }
}

sealed interface ImageSource : AutoCloseable

interface UniFileSource : ImageSource {
    val source: UniFile
}

interface ByteBufferSource : ImageSource {
    val source: ByteBuffer
}

private external fun detectBorder(bitmap: Bitmap): IntArray
