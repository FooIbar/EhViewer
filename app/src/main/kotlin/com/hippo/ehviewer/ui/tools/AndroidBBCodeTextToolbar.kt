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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import androidx.compose.ui.text.input.getTextAfterSelection
import androidx.compose.ui.text.input.getTextBeforeSelection
import androidx.compose.ui.text.style.TextDecoration
import com.hippo.ehviewer.R
import com.hippo.ehviewer.util.findActivity
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

fun AnnotatedString.toBBCode() = buildString {
    var current = 0
    spanStyles.sortedBy { it.start }.forEach {
        append(text.subSequence(current, it.start))
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
        current = it.end
    }
    append(text.subSequence(current, text.length))
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
                                    tfv = TextFieldValue(
                                        buildAnnotatedString {
                                            val len = capturedTfv.text.length
                                            fun addStyle(style: SpanStyle) {
                                                append(capturedTfv.annotatedString)
                                                addStyle(style, capturedTfv.selection.start, capturedTfv.selection.end)
                                            }
                                            when (p1.itemId) {
                                                R.id.action_bold -> addStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                                R.id.action_italic -> addStyle(SpanStyle(fontStyle = FontStyle.Italic))
                                                R.id.action_underline -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                                                R.id.action_strikethrough -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                                                R.id.action_url -> {}
                                                R.id.action_clear -> {
                                                    append(capturedTfv.getTextBeforeSelection(len))
                                                    append(capturedTfv.getSelectedText().text)
                                                    append(capturedTfv.getTextAfterSelection(len))
                                                }
                                            }
                                        },
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
