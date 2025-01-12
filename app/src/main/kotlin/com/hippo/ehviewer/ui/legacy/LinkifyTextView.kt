/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.ui.legacy

import android.annotation.SuppressLint
import android.content.Context
import android.text.Spanned
import android.text.style.URLSpan
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.getSpans

@SuppressLint("ViewConstructor")
class LinkifyTextView(
    context: Context,
    private val onUrlClick: (String) -> Unit,
) : AppCompatTextView(context) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Let the parent or grandparent of TextView to handles click aciton.
        // Otherwise click effect like ripple will not work, and if touch area
        // do not contain a url, the TextView will still get MotionEvent.
        // onTouchEven must be called with MotionEvent.ACTION_DOWN for each touch
        // action on it, so we analyze touched url here.
        if (event.action == MotionEvent.ACTION_DOWN) {
            (text as? Spanned)?.let { text ->
                layout?.let { layout ->
                    val x = event.x + scrollX - totalPaddingLeft
                    val y = event.y + scrollY - totalPaddingTop
                    val line = layout.getLineForVertical(y.toInt())
                    val off = layout.getOffsetForHorizontal(line, x)
                    val spans = text.getSpans<URLSpan>(off, off)
                    if (spans.isNotEmpty()) {
                        onUrlClick(spans[0].url)
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
