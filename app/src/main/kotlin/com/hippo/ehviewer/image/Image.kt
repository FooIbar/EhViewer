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
import android.graphics.ImageDecoder.Source
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import coil3.decode.BitmapFactoryDecoder
import coil3.decode.DecodeUtils
import coil3.decode.ImageSource
import coil3.request.Options
import coil3.size.Scale
import coil3.size.Size
import com.hippo.ehviewer.R
import com.hippo.ehviewer.jni.isGif
import com.hippo.ehviewer.jni.mmap
import com.hippo.ehviewer.jni.munmap
import com.hippo.ehviewer.jni.rewriteGifSource
import com.hippo.ehviewer.util.isAtLeastP
import com.hippo.ehviewer.util.isAtLeastU
import com.hippo.unifile.UniFile
import com.hippo.unifile.openInputStream
import java.nio.ByteBuffer
import kotlin.math.min
import okio.FileSystem
import okio.buffer
import okio.source
import splitties.init.appCtx

class Image private constructor(drawable: Drawable, private val src: AutoCloseable) {
    var mObtainedDrawable: Drawable? = drawable
        private set

    val size = drawable.run { intrinsicHeight * intrinsicWidth * 4 * if (this is Animatable) 4 else 1 }

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

        suspend fun decode(src: AutoCloseable): Image? {
            return runCatching {
                when (src) {
                    is UniFileSource -> {
                        if (isAtLeastP) {
                            if (!isAtLeastU) {
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
                            val drawable = decodeDrawable(src.source.imageSource)
                            if (drawable !is Animatable) src.close()
                            Image(drawable, src)
                        } else {
                            val options = Options(
                                appCtx,
                                size = Size(targetWidth, targetHeight),
                                scale = Scale.FILL,
                                allowInexactSize = true,
                            )
                            val drawable = ImageSource(
                                src.source.openInputStream().source().buffer(),
                                FileSystem.SYSTEM,
                            ).use {
                                BitmapFactoryDecoder(it, options).decode().image.asDrawable(appCtx.resources)
                            }
                            src.close()
                            Image(drawable, src)
                        }
                    }

                    is ByteBufferSource -> {
                        if (isAtLeastP) {
                            if (!isAtLeastU) {
                                rewriteGifSource(src.source)
                            }
                            val source = ImageDecoder.createSource(src.source)
                            val drawable = decodeDrawable(source)
                            if (drawable !is Animatable) src.close()
                            Image(drawable, src)
                        } else {
                            TODO("Unsupported")
                        }
                    }

                    else -> TODO("Unsupported")
                }
            }.onFailure {
                src.close()
                it.printStackTrace()
            }.getOrNull()
        }

        @RequiresApi(Build.VERSION_CODES.P)
        private fun decodeDrawable(src: Source) = ImageDecoder.decodeDrawable(src) { decoder, info, _ ->
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                // Allocating hardware bitmap may cause a crash on framework versions prior to Android Q
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            decoder.setTargetColorSpace(colorSpace)
            decoder.setTargetSampleSize(calculateSampleSize(info, targetHeight, targetWidth))
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
