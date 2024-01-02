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
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.graphics.ImageDecoder.ImageInfo
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toDrawable
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import coil3.decode.DecodeUtils
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.colorSpace
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
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
import kotlin.math.min
import splitties.init.appCtx

private val CropAspect = 0.1..100.0

class Image private constructor(drawable: Drawable, private val src: AutoCloseable) {
    val size = drawable.run { intrinsicHeight * intrinsicWidth * 4 * if (this is Animatable) 4 else 1 }
    private val intrinsicRect = drawable.run { Rect(0, 0, intrinsicWidth, intrinsicHeight) }

    var mObtainedDrawable: Drawable? = if (drawable is Animatable) {
        // Cannot crop animated image's border
        drawable
    } else {
        if (Settings.cropBorder.value) {
            val bitmap = (drawable as BitmapDrawable).bitmap
            val array = detectBorder(bitmap)
            val rect = with(bitmap) {
                val r = Rect(array[0], array[1], array[2], array[3])
                val aspectAfterCrop = r.height().toFloat() / r.width()
                if (aspectAfterCrop in CropAspect) r else intrinsicRect
            }

            val final = if (isAtLeastQ) {
                // Upload to Graphical Buffer to accelerate render
                bitmap.copy(Bitmap.Config.HARDWARE, false).apply {
                    bitmap.recycle()
                }
            } else {
                bitmap
            }

            final.toDrawable(appCtx.resources).apply { bounds = rect }
        } else {
            drawable.apply { bounds = intrinsicRect }
        }
    }

    @Synchronized
    fun recycle() {
        (mObtainedDrawable as? Animatable)?.stop()
        (mObtainedDrawable as? BitmapDrawable)?.bitmap?.recycle()
        mObtainedDrawable?.callback = null
        if (mObtainedDrawable is Animatable) src.close()
        mObtainedDrawable = null
    }

    companion object {
        @RequiresApi(Build.VERSION_CODES.P)
        fun calculateSampleSize(info: ImageInfo, targetHeight: Int, targetWeight: Int): Int {
            return min(
                info.size.width / targetWeight,
                info.size.height / targetHeight,
            ).coerceAtLeast(1)
        }

        private val imageSearchMaxSize = appCtx.resources.getDimensionPixelOffset(R.dimen.image_search_max_size)

        @delegate:RequiresApi(Build.VERSION_CODES.P)
        val imageSearchDecoderSampleListener by lazy {
            ImageDecoder.OnHeaderDecodedListener { decoder, info, _ ->
                decoder.setTargetSampleSize(
                    calculateSampleSize(info, imageSearchMaxSize, imageSearchMaxSize),
                )
            }
        }
        private val targetWidth = appCtx.resources.displayMetrics.widthPixels * 3 / 2
        private val targetHeight = appCtx.resources.displayMetrics.heightPixels * 3 / 2

        @delegate:RequiresApi(Build.VERSION_CODES.O)
        val isWideColorGamut by lazy { appCtx.resources.configuration.isScreenWideColorGamut }

        @RequiresApi(Build.VERSION_CODES.O)
        lateinit var colorSpace: ColorSpace

        suspend fun Either<ByteBufferSource, UniFileSource>.decodeCoil(): Drawable {
            val req = appCtx.imageRequest {
                onLeft { data(it.source) }
                onRight { data(it.source.uri) }
                size(Size(targetWidth, targetHeight))
                scale(Scale.FILL)
                precision(Precision.INEXACT)
                if (isAtLeastO) {
                    colorSpace(colorSpace)
                }
                if (Settings.cropBorder.value) {
                    allowHardware(false)
                }
            }
            val image = when (val result = appCtx.imageLoader.execute(req)) {
                is SuccessResult -> result.image
                is ErrorResult -> throw result.throwable
            }
            return image.asDrawable(appCtx.resources)
        }

        suspend fun decode(src: AutoCloseable): Image? {
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
                        val drawable = src.right().decodeCoil()
                        if (drawable !is Animatable) src.close()
                        Image(drawable, src)
                    }

                    is ByteBufferSource -> {
                        if (isAtLeastP && !isAtLeastU) {
                            rewriteGifSource(src.source)
                        }
                        val drawable = src.left().decodeCoil()
                        if (drawable !is Animatable) src.close()
                        Image(drawable, src)
                    }

                    else -> TODO("Unsupported")
                }
            }.onFailure {
                src.close()
                it.printStackTrace()
            }.getOrNull()
        }

        fun Context.decodeBitmap(uri: Uri): Bitmap? = if (isAtLeastP) {
            val src = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(src, imageSearchDecoderSampleListener)
        } else {
            contentResolver.openFileDescriptor(uri, "r")!!.use {
                val options = BitmapFactory.Options()

                // Disable these since we straight up compress the bitmap to JPEG
                options.inScaled = false
                options.inPremultiplied = false

                options.inJustDecodeBounds = true
                BitmapFactory.decodeFileDescriptor(it.fileDescriptor, null, options)
                options.inJustDecodeBounds = false

                options.inSampleSize = DecodeUtils.calculateInSampleSize(
                    options.outWidth,
                    options.outHeight,
                    imageSearchMaxSize,
                    imageSearchMaxSize,
                    Scale.FILL,
                )
                BitmapFactory.decodeFileDescriptor(it.fileDescriptor, null, options)
            }
        }
    }

    interface UniFileSource : AutoCloseable {
        val source: UniFile
    }

    interface ByteBufferSource : AutoCloseable {
        val source: ByteBuffer
    }
}

private external fun detectBorder(bitmap: Bitmap): IntArray
