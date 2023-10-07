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
import coil.decode.BitmapFactoryDecoder
import coil.decode.DecodeUtils
import coil.decode.GifDecoder
import coil.decode.ImageSource
import coil.decode.isGif
import coil.request.Options
import com.hippo.ehviewer.R
import com.hippo.ehviewer.util.isAtLeastP
import com.hippo.unifile.UniFile
import com.hippo.unifile.openInputStream
import okio.buffer
import okio.source
import splitties.init.appCtx
import java.nio.ByteBuffer
import kotlin.math.min

class Image private constructor(private val src: AutoCloseable) {
    var mObtainedDrawable: Drawable? = null
        private set

    val size: Int
        get() = mObtainedDrawable!!.run { intrinsicHeight * intrinsicWidth * 4 * if (this is Animatable) 4 else 1 }

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
        private val screenWidth = appCtx.resources.displayMetrics.widthPixels
        private val screenHeight = appCtx.resources.displayMetrics.heightPixels

        @delegate:RequiresApi(Build.VERSION_CODES.O)
        val isWideColorGamut by lazy { appCtx.resources.configuration.isScreenWideColorGamut }

        @RequiresApi(Build.VERSION_CODES.O)
        lateinit var colorSpace: ColorSpace

        suspend fun decode(src: UniFileSource): Image? {
            return runCatching {
                Image(src).apply {
                    mObtainedDrawable = if (isAtLeastP) {
                        decodeDrawable(src.source.imageSource)
                    } else {
                        ImageSource(src.source.openInputStream().source().buffer(), appCtx).use {
                            val options = Options(appCtx)
                            if (DecodeUtils.isGif(it.source())) {
                                GifDecoder(it, options).decode().drawable
                            } else {
                                BitmapFactoryDecoder(it, options).decode().drawable
                            }
                        }
                    }.also {
                        if (it !is Animatable) src.close()
                    }
                }
            }.onFailure {
                src.close()
                it.printStackTrace()
            }.getOrNull()
        }

        fun decode(src: ByteBufferSource) = runCatching {
            Image(src).apply {
                mObtainedDrawable = if (isAtLeastP) {
                    val source = ImageDecoder.createSource(src.source)
                    decodeDrawable(source)
                } else {
                    null
                }.also {
                    if (it !is Animatable) src.close()
                }
            }
        }.onFailure {
            src.close()
            it.printStackTrace()
        }.getOrNull()

        @RequiresApi(Build.VERSION_CODES.P)
        private fun decodeDrawable(src: Source) = ImageDecoder.decodeDrawable(src) { decoder, info, _ ->
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                // Allocating hardware bitmap may cause a crash on framework versions prior to Android Q
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            decoder.setTargetColorSpace(colorSpace)
            decoder.setTargetSampleSize(
                calculateSampleSize(info, 2 * screenHeight, 2 * screenWidth),
            )
        }
    }

    interface UniFileSource : AutoCloseable {
        val source: UniFile
    }

    interface ByteBufferSource : AutoCloseable {
        val source: ByteBuffer
    }
}

fun Context.decodeBitmap(uri: Uri): Bitmap? = if (isAtLeastP) {
    val src = ImageDecoder.createSource(contentResolver, uri)
    ImageDecoder.decodeBitmap(src, Image.imageSearchDecoderSampleListener)
} else {
    contentResolver.openFileDescriptor(uri, "r")!!.use {
        BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
    }
}

external fun rewriteGifSource(buffer: ByteBuffer)
external fun rewriteGifSource2(fd: Int)
