package com.hippo.ehviewer.ui.tools

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.input.TextFieldValue

typealias PlatformRect = android.graphics.Rect

@Composable
fun rememberBBCodeTextToolbar(textFieldValue: TextFieldValue): TextToolbar {
    val tfv by rememberUpdatedState(newValue = textFieldValue)
    val view = LocalView.current
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
                                // TODO("Not yet implemented")
                                return true
                            }

                            override fun onPrepareActionMode(p0: ActionMode, p1: Menu): Boolean {
                                // TODO("Not yet implemented")
                                return true
                            }

                            override fun onActionItemClicked(p0: ActionMode, p1: MenuItem): Boolean {
                                // TODO("Not yet implemented")
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
