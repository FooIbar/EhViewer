package com.hippo.ehviewer.ui.reader

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

fun Modifier.keyEventHandler(
    enabled: () -> Boolean,
    movePrevious: () -> Unit,
    moveNext: () -> Unit,
) = onPreviewKeyEvent {
    if (enabled() && it.type == KeyEventType.KeyDown) {
        when (it.key) {
            Key.DirectionUp, Key.PageUp, Key.VolumeUp -> movePrevious()
            Key.DirectionDown, Key.PageDown, Key.VolumeDown -> moveNext()
            else -> return@onPreviewKeyEvent false
        }
        true
    } else {
        false
    }
}
