package com.hippo.ehviewer.ui.tools

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import androidx.compose.ui.text.input.getTextAfterSelection
import androidx.compose.ui.text.input.getTextBeforeSelection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.hippo.ehviewer.R
import com.hippo.ehviewer.util.findActivity
import com.hippo.ehviewer.util.toRangeSet
import io.github.petertrr.diffutils.diffInline
import io.github.petertrr.diffutils.patch.ChangeDelta
import io.github.petertrr.diffutils.patch.DeleteDelta
import io.github.petertrr.diffutils.patch.EqualDelta
import io.github.petertrr.diffutils.patch.InsertDelta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.tarsin.kt.unreachable

typealias PlatformRect = android.graphics.Rect

object NoopClipboardManager : ClipboardManager {
    override fun getText() = null
    override fun setText(annotatedString: AnnotatedString) = Unit
}

fun TextFieldValue.updateSpan(origin: TextFieldValue): TextFieldValue {
    val oriSpan = origin.annotatedString.spanStyles
    val annoStr = origin.annotatedString
    val len = origin.text.length
    if (oriSpan.isEmpty()) {
        // User have no spanned comment, just update
        return this
    }
    // Hacky: BasicTextField would clear text spans
    val diff = diffInline(origin.text, text).deltas
    if (diff.isNotEmpty()) {
        if (diff.size == 1) {
            val str = when (val delta = diff.first()) {
                is DeleteDelta -> {
                    val pos = delta.source.position
                    val toIns = delta.source.lines.first()
                    val end = pos + toIns.length // Exclusive
                    buildAnnotatedString {
                        append(annoStr.subSequence(0, pos))
                        append(annoStr.subSequence(end, len))
                    }
                }
                is ChangeDelta -> unreachable()
                is EqualDelta -> unreachable()
                is InsertDelta -> {
                    val pos = delta.source.position
                    val toIns = delta.target.lines.first()
                    buildAnnotatedString {
                        append(annoStr.subSequence(0, pos))
                        append(toIns)
                        append(annoStr.subSequence(pos, len))
                    }
                }
            }
            // Update span range for updated comment
            return copy(annotatedString = str)
        } else {
            // Cannot handle diff, override directly
            return this
        }
    } else {
        // Raw Text not changed, copy annotatedString
        return copy(
            annotatedString = origin.annotatedString,
        )
    }
}

inline fun List<AnnotatedString.Range<SpanStyle>>.filterMerged(singleSpan: SpanStyle, predicate: (SpanStyle) -> Boolean) = filter {
    predicate(it.item) && it.start != it.end
}.map { it.start..<it.end }.toRangeSet().map {
    AnnotatedString.Range(singleSpan, it.start, it.endInclusive + 1)
}

fun AnnotatedString.normalizeSpan(): AnnotatedString {
    if (spanStyles.isEmpty()) return this
    val spans = with(spanStyles) {
        val bold = filterMerged(SpanStyle(fontWeight = FontWeight.Bold)) { it.fontWeight == FontWeight.Bold }
        val italic = filterMerged(SpanStyle(fontStyle = FontStyle.Italic)) { it.fontStyle == FontStyle.Italic }
        val underline = filterMerged(SpanStyle(textDecoration = TextDecoration.Underline)) { it.textDecoration == TextDecoration.Underline }
        val linethrough = filterMerged(SpanStyle(textDecoration = TextDecoration.LineThrough)) { it.textDecoration == TextDecoration.LineThrough }
        listOf(bold, italic, underline, linethrough).flatten()
    }
    return AnnotatedString(text, spans)
}

// Overlapped SpanStyle is support through stack based builder
// Does BBCode allow interlaced tags? If not, split interlaced span first?
fun AnnotatedString.toBBCode() = buildString {
    val len = text.length
    fun SpanStyle.push() = when {
        fontWeight == FontWeight.Bold -> append("[b]")
        fontStyle == FontStyle.Italic -> append("[i]")
        textDecoration == TextDecoration.Underline -> append("[u]")
        textDecoration == TextDecoration.LineThrough -> append("[s]")
        else -> this@buildString
    }
    fun SpanStyle.pop() = when {
        fontWeight == FontWeight.Bold -> append("[/b]")
        fontStyle == FontStyle.Italic -> append("[/i]")
        textDecoration == TextDecoration.Underline -> append("[/u]")
        textDecoration == TextDecoration.LineThrough -> append("[/s]")
        else -> this@buildString
    }
    var current = 0
    val stack = ArrayDeque<AnnotatedString.Range<SpanStyle>>()
    val spans = spanStyles.groupBy { it.start }
    while (true) {
        while (stack.lastOrNull()?.end == current)
            stack.removeLast().item.pop()
        if (current == len) break
        spans[current]?.sortedByDescending { it.end }?.forEach {
            it.item.push()
            stack.addLast(it)
        }
        append(text[current])
        current++
    }
}

fun Spanned.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    val spanned = this@toAnnotatedString
    append(spanned.toString())
    getSpans(0, spanned.length, Any::class.java).forEach { span ->
        val start = getSpanStart(span)
        val end = getSpanEnd(span)
        fun addStyle(spanStyle: SpanStyle) = addStyle(spanStyle, start, end)
        when (span) {
            is StrikethroughSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
            is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline))
            is StyleSpan -> {
                val s = span.style
                if (s and Typeface.BOLD != 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold))
                if (s and Typeface.ITALIC != 0) addStyle(SpanStyle(fontStyle = FontStyle.Italic))
            }
        }
    }
}

@Composable
fun rememberBBCodeTextToolbar(textFieldValue: MutableState<TextFieldValue>): TextToolbar {
    var tfv by textFieldValue
    val view = LocalView.current
    val context = LocalContext.current
    val activity = remember { context.findActivity<ComponentActivity>() }
    val coroutineScope = rememberCoroutineScope()
    val toolbar = remember {
        object : TextToolbar {
            private var actionMode: ActionMode? = null
            override var status = TextToolbarStatus.Hidden

            override fun hide() {
                status = TextToolbarStatus.Hidden
                actionMode?.finish()
                actionMode = null
            }

            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?,
            ) {
                if (actionMode == null) {
                    status = TextToolbarStatus.Shown
                    actionMode = view.startActionMode(
                        object : ActionMode.Callback2() {
                            override fun onCreateActionMode(p0: ActionMode, p1: Menu): Boolean {
                                activity.menuInflater.inflate(R.menu.context_comment, p1)
                                return true
                            }

                            override fun onPrepareActionMode(p0: ActionMode, p1: Menu): Boolean {
                                return true
                            }

                            override fun onActionItemClicked(p0: ActionMode, p1: MenuItem): Boolean {
                                val capturedTfv = tfv
                                coroutineScope.launch {
                                    // Hacky: Let TextField recompose first
                                    delay(100)
                                    // Reversed range is not supported by AnnotatedString
                                    val start = capturedTfv.selection.start.coerceAtMost(capturedTfv.selection.end)
                                    val end = capturedTfv.selection.start.coerceAtLeast(capturedTfv.selection.end)
                                    tfv = TextFieldValue(
                                        buildAnnotatedString {
                                            val len = capturedTfv.text.length
                                            fun addStyle(style: SpanStyle) {
                                                append(capturedTfv.annotatedString)
                                                addStyle(style, start, end)
                                            }
                                            fun addTextDecoration(decoration: TextDecoration) {
                                                append(capturedTfv.getTextBeforeSelection(len))
                                                val ans = capturedTfv.getSelectedText()
                                                val spans = ans.spanStyles.filter {
                                                    it.item.textDecoration == null || it.item.textDecoration == decoration
                                                }
                                                withStyle(SpanStyle(textDecoration = decoration)) {
                                                    append(AnnotatedString(ans.text, spans))
                                                }
                                                append(capturedTfv.getTextAfterSelection(len))
                                            }
                                            when (p1.itemId) {
                                                R.id.action_bold -> addStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                                R.id.action_italic -> addStyle(SpanStyle(fontStyle = FontStyle.Italic))
                                                R.id.action_underline -> addTextDecoration(TextDecoration.Underline)
                                                R.id.action_strikethrough -> addTextDecoration(TextDecoration.LineThrough)
                                                R.id.action_url -> append(capturedTfv.annotatedString)
                                                R.id.action_clear -> {
                                                    append(capturedTfv.getTextBeforeSelection(len))
                                                    append(capturedTfv.getSelectedText().text)
                                                    append(capturedTfv.getTextAfterSelection(len))
                                                }
                                            }
                                        },
                                        selection = TextRange(end),
                                    )
                                }

                                // Hacky: Notify BasicTextField clear state
                                onCopyRequested?.invoke()
                                return true
                            }

                            override fun onDestroyActionMode(p0: ActionMode) {
                                actionMode = null
                            }

                            override fun onGetContentRect(mode: ActionMode, view: View, outRect: PlatformRect) {
                                outRect.set(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
                            }
                        },
                        ActionMode.TYPE_FLOATING,
                    )
                } else {
                    actionMode?.invalidate()
                }
            }
        }
    }
    return toolbar
}
