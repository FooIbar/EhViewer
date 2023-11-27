package rikka.core.res

import android.content.res.Resources
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

@ColorInt
fun Resources.Theme.resolveColor(@AttrRes attrId: Int): Int {
    val a = obtainStyledAttributes(intArrayOf(attrId))
    val res = a.getColor(0, 0)
    a.recycle()
    return res
}
