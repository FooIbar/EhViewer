package com.hippo.ehviewer.ui.legacy

import android.graphics.drawable.DrawableWrapper
import android.text.Html
import coil3.asDrawable
import coil3.decode.DecodeUtils
import coil3.imageLoader
import coil3.request.crossfade
import coil3.size.Scale
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.util.toIntOrDefault
import kotlin.math.roundToInt
import splitties.init.appCtx

private val UrlRegex = "(?:[^-]+-){2}(\\d+)-(\\d+)-[^_]+_([^.]+)".toRegex()

class CoilImageGetter(private val onSuccess: () -> Unit) : Html.ImageGetter {
    override fun getDrawable(source: String) = object : DrawableWrapper(null) {}.apply {
        UrlRegex.find(source)?.run {
            val srcWidth = groupValues[1].toInt()
            val srcHeight = groupValues[2].toInt()
            val dstWidth = groupValues[3].toIntOrDefault(200)
            val dstHeight = dstWidth / 2 * 3
            val multiplier = DecodeUtils.computeSizeMultiplier(srcWidth, srcHeight, dstWidth, dstHeight, Scale.FIT)
            setBounds(0, 0, (srcWidth * multiplier).roundToInt(), (srcHeight * multiplier).roundToInt())
        } ?: setBounds(0, 0, 200, 300)
        with(appCtx) {
            imageLoader.enqueue(
                imageRequest {
                    data(source)
                    crossfade(false)
                    target { image ->
                        setDrawable(image.asDrawable(resources))
                        onSuccess()
                    }
                },
            )
        }
    }
}
