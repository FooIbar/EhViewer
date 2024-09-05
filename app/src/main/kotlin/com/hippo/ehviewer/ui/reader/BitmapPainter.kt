package com.hippo.ehviewer.ui.reader

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.util.fastRoundToInt

class BitmapPainter(private val bitmap: Bitmap, srcRect: IntRect) : Painter() {

    private val srcRect = with(srcRect) {
        RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    }

    private val dstRect = RectF()

    private val matrix = Matrix()

    // Use the overload that takes a `Matrix` to bypass the 100 MB size limit
    override fun DrawScope.onDraw() = drawIntoCanvas {
        dstRect.right = size.width.fastRoundToInt().toFloat()
        dstRect.bottom = size.height.fastRoundToInt().toFloat()
        matrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.FILL)
        it.nativeCanvas.drawBitmap(bitmap, matrix, paint)
    }

    override val intrinsicSize get() = Size(srcRect.width(), srcRect.height())

    companion object {
        private val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
    }
}
