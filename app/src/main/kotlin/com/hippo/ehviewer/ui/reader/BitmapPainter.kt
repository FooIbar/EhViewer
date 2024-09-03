package com.hippo.ehviewer.ui.reader

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize

class BitmapPainter(
    private val image: Bitmap,
    srcOffset: IntOffset = IntOffset.Zero,
    srcSize: IntSize = IntSize(image.width, image.height),
) : Painter() {

    private val offset = (-srcOffset).toOffset()

    private val srcSize = srcSize.toSize()

    private val matrix = Matrix()

    override fun DrawScope.onDraw() = drawIntoCanvas {
        // Use the overload that takes a `Matrix` to bypass the 100 MB size limit
        it.nativeCanvas.drawBitmap(
            image,
            matrix.apply {
                setTranslate(offset.x, offset.y)
                postScale(size.width / srcSize.width, size.height / srcSize.height)
            },
            bitmapPaint,
        )
    }

    override val intrinsicSize get() = srcSize

    companion object {
        private val bitmapPaint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
    }
}
