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
    private var timeMark = TimeSource.Monotonic.markNow()
    private var frameDuration = 0
    private var currentFrame: Frame
    private var nextFrame: Frame

    init {
        check(decoder != 0L) { "Failed to create decoder" }
        val packed = nativeGetImageInfo(decoder)
        width = (packed shr 40).toInt()
        height = (packed shr 16 and 0xFFFFFF).toInt()
        loopCount = (packed and 0xFFFF).toInt()
        val bitmap = createBitmap(width, height)
        val timestamp = nativeDecodeNextFrame(decoder, bitmap)
        check(timestamp != 0) {
            nativeDestroyDecoder(decoder)
            "Failed to decode first frame"
        }
        currentFrame = Frame(bitmap, 0)
        nextFrame = Frame(createBitmap(width, height), timestamp)
    }

    override fun getIntrinsicWidth() = width

    override fun getIntrinsicHeight() = height

    private fun checkDecodeResult() {
        if (nextFrame.timestamp == 0) {
            throw CancellationException("Failed to decode next frame")
        }
    }

    override fun draw(canvas: Canvas) {
        if (decodeJob?.isCompleted == true && nextFrame.timestamp != 0) {
            timeMark = TimeSource.Monotonic.markNow()
            frameDuration = nextFrame.timestamp - currentFrame.timestamp
            currentFrame = nextFrame.also { nextFrame = currentFrame }
            decodeJob = if (loopCount == 0 || loopsCompleted < loopCount) {
                decodeScope.launch {
                    nextFrame.timestamp = nativeDecodeNextFrame(decoder, nextFrame.bitmap)
                    checkDecodeResult()
                    if (nextFrame.timestamp < currentFrame.timestamp) {
                        currentFrame.timestamp = 0
                        loopsCompleted++
                    }
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
            timeMark = TimeSource.Monotonic.markNow()
            frameDuration = nextFrame.timestamp - currentFrame.timestamp
            currentFrame.timestamp = nextFrame.timestamp
            loopsCompleted = 0
            decodeJob = decodeScope.launch {
                if (currentFrame.timestamp != frameDuration) nativeResetDecoder(decoder)
                nextFrame.timestamp = nativeDecodeNextFrame(decoder, nextFrame.bitmap)
                checkDecodeResult()
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
private external fun nativeDecodeNextFrame(decoder: Long, bitmap: Bitmap): Int
private external fun nativeDestroyDecoder(decoder: Long)
private external fun nativeResetDecoder(decoder: Long)
