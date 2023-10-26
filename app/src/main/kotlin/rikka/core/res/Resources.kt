package rikka.core.res

import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

@ColorInt
fun Resources.Theme.resolveColor(@AttrRes attrId: Int): Int {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getColor(0, 0)
    a.recycle()
    return res
}

fun Resources.Theme.resolveDrawable(@AttrRes attrId: Int): Drawable? {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getDrawable(0)
    a.recycle()
    return res
}
