package com.hippo.ehviewer.coil

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.core.graphics.createBitmap
import eu.kanade.tachiyomi.util.system.logcat
import java.nio.ByteBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
    private var timeToShowNextFrame = 0L
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
        currentFrame = Frame(bitmap, timestamp)
        nextFrame = Frame(createBitmap(width, height), 0)
    }

    override fun getIntrinsicWidth() = width

    override fun getIntrinsicHeight() = height

    private val runnable = Runnable {
        timeToShowNextFrame = SystemClock.uptimeMillis() + nextFrame.timestamp - currentFrame.timestamp
        invalidateSelf()
    }

    private fun decodeNextFrame(reset: Boolean) = decodeScope.launch {
        val timestamp = nativeDecodeNextFrame(decoder, reset, nextFrame.bitmap)
        ensureActive()
        check(timestamp != 0) {
            decodeJob = null
            "Failed to decode next frame"
        }
        if (timestamp <= currentFrame.timestamp) {
            if (reset) loopsCompleted = 0 else loopsCompleted++
            currentFrame.timestamp = 0
        }
        nextFrame.timestamp = timestamp
        nextFrame.bitmap.prepareToDraw()
    }.apply {
        invokeOnCompletion { cause ->
            when (cause) {
                null -> if (reset) {
                    runnable.run()
                } else {
                    scheduleSelf(runnable, timeToShowNextFrame)
                }
                !is CancellationException -> logcat(cause)
            }
        }
    }

    override fun draw(canvas: Canvas) {
        if (decodeJob?.isCompleted == true && isVisible) {
            decodeJob = if (loopCount == 0 || loopsCompleted < loopCount) {
                currentFrame = nextFrame.also { nextFrame = currentFrame }
                decodeNextFrame(false)
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
            decodeJob = decodeNextFrame(true)
        }
    }

    override fun stop() {
        decodeJob?.cancel()
        decodeJob = null
        unscheduleSelf(runnable)
    }

    fun dispose() {
        runBlocking {
            decodeScope.coroutineContext.job.cancelAndJoin()
        }
        nativeDestroyDecoder(decoder)
    }
}

private class Frame(val bitmap: Bitmap, var timestamp: Int)

private external fun nativeCreateDecoder(source: ByteBuffer): Long
private external fun nativeGetImageInfo(decoder: Long): Long
private external fun nativeDecodeNextFrame(decoder: Long, reset: Boolean, bitmap: Bitmap): Int
private external fun nativeDestroyDecoder(decoder: Long)
