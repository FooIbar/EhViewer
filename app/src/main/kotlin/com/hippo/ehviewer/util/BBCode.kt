package com.hippo.ehviewer.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

fun AnnotatedString.toBBCode() = buildString {
    var start = 0
    spanStyles.sortedBy { it.start }.forEach {
        val prev = it.start - 1
        if (prev >= start) append(text.subSequence(start, prev))
        with(it.item) {
            when {
                fontWeight == FontWeight.Bold -> append("[b]")
                fontStyle == FontStyle.Italic -> append("[i]")
                textDecoration == TextDecoration.Underline -> append("[u]")
                textDecoration == TextDecoration.LineThrough -> append("[s]")
                else -> Unit
            }
        }
        append(text.subSequence(it.start, it.end))
        with(it.item) {
            when {
                fontWeight == FontWeight.Bold -> append("[/b]")
                fontStyle == FontStyle.Italic -> append("[/i]")
                textDecoration == TextDecoration.Underline -> append("[/u]")
                textDecoration == TextDecoration.LineThrough -> append("[/s]")
                else -> Unit
            }
        }
        start = it.end + 1
    }
    if (start <= text.length) append(text.subSequence(start - 1, text.length))
}
