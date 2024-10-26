package com.hippo.ehviewer.ui.reader

import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_UP
import android.view.KeyEvent.KEYCODE_VOLUME_DOWN
import android.view.KeyEvent.KEYCODE_VOLUME_UP
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import com.hippo.ehviewer.Settings

@Composable
fun KeyEventHandler(
    enabled: () -> Boolean,
    movePrevious: () -> Unit,
    moveNext: () -> Unit,
) {
    val view = LocalView.current
    DisposableEffect(view) {
        val listener = ViewCompat.OnUnhandledKeyEventListenerCompat { _, event ->
            val isUp = event.action == ACTION_UP
            when (val keyCode = event.keyCode) {
                KEYCODE_VOLUME_UP, KEYCODE_VOLUME_DOWN -> {
                    if (!enabled()) return@OnUnhandledKeyEventListenerCompat false
                    val interval = Settings.readWithVolumeKeysInterval.value + 1
                    val inverted = Settings.readWithVolumeKeysInverted.value
                    if (!isUp && event.repeatCount % interval == 0) {
                        if (inverted.xor(keyCode == KEYCODE_VOLUME_UP)) {
                            movePrevious()
                        } else {
                            moveNext()
                        }
                    }
                }

                KEYCODE_DPAD_UP -> if (isUp) movePrevious()
                KEYCODE_DPAD_DOWN -> if (isUp) moveNext()
                else -> return@OnUnhandledKeyEventListenerCompat false
            }
            true
        }
        ViewCompat.addOnUnhandledKeyEventListener(view, listener)
        onDispose {
            ViewCompat.removeOnUnhandledKeyEventListener(view, listener)
        }
    }
}
