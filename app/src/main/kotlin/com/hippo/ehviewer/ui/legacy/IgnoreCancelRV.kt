package com.hippo.ehviewer.ui.legacy

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class IgnoreCancelRV @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : RecyclerView(context, attrs, defStyle) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        // Intercept MotionEvent.ACTION_CANCEL for rv have SelectionTracker attached
        // Otherwise predictive back will not work
        if (e.action == MotionEvent.ACTION_CANCEL) return false
        return super.onTouchEvent(e)
    }
}
