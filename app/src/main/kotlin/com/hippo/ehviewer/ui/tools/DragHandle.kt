package com.hippo.ehviewer.ui.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import sh.calvin.reorderable.ReorderableCollectionItemScope

@Composable
fun ReorderableCollectionItemScope.DragHandle(
    hapticFeedback: HapticFeedback,
    modifier: Modifier = Modifier,
    onDragStarted: () -> Unit = {},
    onDragStopped: () -> Unit = {},
) {
    IconButton(
        onClick = {},
        modifier = modifier.draggableHandle(
            onDragStarted = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.START)
                onDragStarted()
            },
            onDragStopped = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.END)
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
