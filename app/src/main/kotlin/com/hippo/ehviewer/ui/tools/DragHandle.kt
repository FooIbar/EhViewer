package com.hippo.ehviewer.ui.tools

import android.view.HapticFeedbackConstants
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.hippo.ehviewer.util.isAtLeastOMR1
import com.hippo.ehviewer.util.isAtLeastR
import com.hippo.ehviewer.util.isAtLeastU
import sh.calvin.reorderable.ReorderableItemScope

@Composable
fun ReorderableItemScope.DragHandle(
    modifier: Modifier = Modifier,
    onDragStarted: () -> Unit = {},
    onDragStopped: () -> Unit = {},
) {
    val view = LocalView.current
    IconButton(
        onClick = {},
        modifier = modifier.draggableHandle(
            onDragStarted = {
                val feedbackConstant = if (isAtLeastU) {
                    HapticFeedbackConstants.DRAG_START
                } else if (isAtLeastR) {
                    HapticFeedbackConstants.GESTURE_START
                } else if (isAtLeastOMR1) {
                    HapticFeedbackConstants.KEYBOARD_PRESS
                } else {
                    HapticFeedbackConstants.VIRTUAL_KEY
                }
                view.performHapticFeedback(feedbackConstant)
                onDragStarted()
            },
            onDragStopped = {
                val feedbackConstant = if (isAtLeastR) {
                    HapticFeedbackConstants.GESTURE_END
                } else if (isAtLeastOMR1) {
                    HapticFeedbackConstants.KEYBOARD_RELEASE
                } else {
                    HapticFeedbackConstants.VIRTUAL_KEY_RELEASE
                }
                view.performHapticFeedback(feedbackConstant)
                onDragStopped()
            },
        ),
    ) {
        Icon(
            imageVector = Icons.Default.Reorder,
            contentDescription = null,
        )
    }
}

val draggingHapticFeedback = if (isAtLeastU) {
    HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
} else if (isAtLeastOMR1) {
    HapticFeedbackConstants.TEXT_HANDLE_MOVE
} else {
    HapticFeedbackConstants.CLOCK_TICK
}
