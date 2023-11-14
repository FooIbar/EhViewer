package com.hippo.ehviewer.ui.tools

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
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

typealias PlatformRect = android.graphics.Rect

object NoopClipboardManager : ClipboardManager {
    override fun getText() = null
    override fun setText(annotatedString: AnnotatedString) = Unit
}

@Composable
fun rememberBBCodeTextToolbar(textFieldValue: MutableState<TextFieldValue>): TextToolbar {
    var tfv by textFieldValue
    val view = LocalView.current
    val context = LocalContext.current
    val activity = remember { context.findActivity<ComponentActivity>() }
    val clipboardManager = LocalClipboardManager.current
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
                                tfv = TextFieldValue(
                                    buildAnnotatedString {
                                        val len = tfv.text.length
                                        fun addStyle(style: SpanStyle) {
                                            append(tfv.annotatedString)
                                            addStyle(style, tfv.selection.start, tfv.selection.end)
                                        }
                                        when (p1.itemId) {
                                            R.id.action_bold -> addStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                            R.id.action_italic -> addStyle(SpanStyle(fontStyle = FontStyle.Italic))
                                            R.id.action_underline -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                                            R.id.action_strikethrough -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                                            R.id.action_url -> TODO()
                                            R.id.action_clear -> {
                                                append(tfv.getTextBeforeSelection(len))
                                                append(tfv.getSelectedText().text)
                                                append(tfv.getTextAfterSelection(len))
                                            }
                                        }
                                    },
                                )

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
