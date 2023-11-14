package com.hippo.ehviewer.util

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.ImageSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
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
    if (start <= text.length) append(text.subSequence(start, text.length))
}

fun Spanned.toBBCode(): String {
    val text = this
    tailrec fun StringBuilder.processOneSpanTransition(cur: Int = 0) {
        val next = nextSpanTransition(cur, text.length, CharacterStyle::class.java)
        getSpans(cur, next, CharacterStyle::class.java).forEach {
            when (it) {
                is StyleSpan -> {
                    val s = it.style
                    if (s and Typeface.BOLD != 0) {
                        append("[b]")
                    }
                    if (s and Typeface.ITALIC != 0) {
                        append("[i]")
                    }
                }

                is UnderlineSpan -> append("[u]")
                is StrikethroughSpan -> append("[s]")
                is URLSpan -> {
                    append("[url=")
                    append(it.url)
                    append("]")
                }

                is ImageSpan -> {
                    append("[img]")
                    append(it.source)
                    append("[/img]")
                }
            }
        }
        append(text.subSequence(cur, next))
        getSpans(cur, next, CharacterStyle::class.java).reversed().forEach {
            when (it) {
                is StyleSpan -> {
                    val s = it.style
                    if (s and Typeface.BOLD != 0) {
                        append("[/b]")
                    }
                    if (s and Typeface.ITALIC != 0) {
                        append("[/i]")
                    }
                }

                is UnderlineSpan -> append("[/u]")
                is StrikethroughSpan -> append("[/s]")
                is URLSpan -> append("[/url]")
            }
        }
        if (next < text.length) processOneSpanTransition(next)
    }
    return buildString {
        processOneSpanTransition()
    }
}
