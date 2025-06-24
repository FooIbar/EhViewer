package com.hippo.ehviewer.coil

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import java.nio.ByteBuffer
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Hold a reference to the buffer as it's used by the decoder
@Suppress("CanBeParameter")
class AnimatedWebPDrawable(private val source: ByteBuffer) : Drawable(), Animatable {
    private val decodeScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val decoder = nativeCreateDecoder(source)
    private val width: Int
    private val height: Int
    private val loopCount: Int

    private var decodeJob: Job? = null
    private var loopsCompleted = 0
    private var frameDuration: Int
    private var currentFrame: Frame
    private var nextFrame: Frame

    init {
        check(decoder != 0L) { "Failed to create decoder" }
        val packed = nativeGetImageInfo(decoder)
        width = (packed shr 40).toInt()
        height = (packed shr 16 and 0xFFFFFF).toInt()
        loopCount = (packed and 0xFFFF).toInt()
        val bitmap = createBitmap(width, height)
        val timestamp = nativeDecodeNextFrame(decoder, false, bitmap)
        check(timestamp != 0) {
            nativeDestroyDecoder(decoder)
            "Failed to decode first frame"
        }
        frameDuration = timestamp
        currentFrame = Frame(bitmap, timestamp)
        nextFrame = Frame(createBitmap(width, height), 0)
    }

    override fun getIntrinsicWidth() = width

    override fun getIntrinsicHeight() = height

    private fun decodeNextFrame(reset: Boolean) {
        nextFrame.timestamp = nativeDecodeNextFrame(decoder, reset, nextFrame.bitmap)
        if (nextFrame.timestamp == 0) {
            throw CancellationException("Failed to decode next frame")
        }
        nextFrame.bitmap.prepareToDraw()
    }

    override fun draw(canvas: Canvas) {
        if (decodeJob?.isCompleted == true && nextFrame.timestamp != 0) {
            val timeMark = TimeSource.Monotonic.markNow()
            frameDuration = if (nextFrame.timestamp > currentFrame.timestamp) {
                nextFrame.timestamp - currentFrame.timestamp
            } else {
                loopsCompleted++
                nextFrame.timestamp
            }
            currentFrame = nextFrame.also { nextFrame = currentFrame }
            decodeJob = if (loopCount == 0 || loopsCompleted < loopCount) {
                decodeScope.launch {
                    decodeNextFrame(false)
                    delay(frameDuration - timeMark.elapsedNow().inWholeMilliseconds)
                    invalidateSelf()
                }
            } else {
                null
            }
        }
        canvas.drawBitmap(currentFrame.bitmap, null, bounds, paint)
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setAlpha(alpha: Int) {
        if (alpha != paint.alpha) {
            paint.alpha = alpha
            invalidateSelf()
        }
    }

    override fun getAlpha(): Int = paint.alpha

    override fun setColorFilter(colorFilter: ColorFilter?) {
        if (colorFilter != paint.colorFilter) {
            paint.colorFilter = colorFilter
            invalidateSelf()
        }
    }

    override fun getColorFilter(): ColorFilter? = paint.colorFilter

    override fun isRunning() = decodeJob != null

    override fun start() {
        if (decodeJob == null) {
            val timeMark = TimeSource.Monotonic.markNow()
            loopsCompleted = 0
            decodeJob = decodeScope.launch {
                val isFirstFrame = currentFrame.timestamp == frameDuration
                decodeNextFrame(!isFirstFrame)
                delay(frameDuration - timeMark.elapsedNow().inWholeMilliseconds)
                invalidateSelf()
            }
        }
    }

    override fun stop() {
        decodeJob?.cancel()
        decodeJob = null
    }

    fun dispose() {
        nativeDestroyDecoder(decoder)
    }
}

private class Frame(val bitmap: Bitmap, var timestamp: Int)

private external fun nativeCreateDecoder(source: ByteBuffer): Long
private external fun nativeGetImageInfo(decoder: Long): Long
private external fun nativeDecodeNextFrame(decoder: Long, reset: Boolean, bitmap: Bitmap): Int
private external fun nativeDestroyDecoder(decoder: Long)
