package com.hippo.ehviewer.ui.legacy

import android.graphics.drawable.Animatable
import android.graphics.drawable.DrawableWrapper
import android.text.Html
import android.widget.TextView
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.crossfade
import coil3.size.Size
import com.hippo.ehviewer.ktbuilder.imageRequest

class CoilImageGetter(
    private val textView: TextView,
) : Html.ImageGetter {
    override fun getDrawable(source: String) = object : DrawableWrapper(null) {}.apply {
        with(textView.context) {
            imageLoader.enqueue(
                imageRequest {
                    data(source)
                    crossfade(false)
                    size(Size.ORIGINAL)
                    target { drawable ->
                        setDrawable(drawable.asDrawable(resources))
                        if (drawable is Animatable) drawable.start()
                        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                        textView.text = textView.text
                    }
                },
            )
        }
    }
}
