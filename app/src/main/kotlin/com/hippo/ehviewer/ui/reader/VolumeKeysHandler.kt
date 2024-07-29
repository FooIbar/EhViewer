package com.hippo.ehviewer.ui.reader

import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.KEYCODE_VOLUME_DOWN
import android.view.KeyEvent.KEYCODE_VOLUME_UP
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import com.hippo.ehviewer.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

context(CoroutineScope)
@Composable
fun VolumeKeysHandler(
    enabled: () -> Boolean,
    movePrevious: suspend () -> Unit,
    moveNext: suspend () -> Unit,
) {
    val view = LocalView.current
    DisposableEffect(view) {
        val listener = ViewCompat.OnUnhandledKeyEventListenerCompat { _, event ->
            if (!enabled()) return@OnUnhandledKeyEventListenerCompat false
            when (val keyCode = event.keyCode) {
                KEYCODE_VOLUME_UP, KEYCODE_VOLUME_DOWN -> {
                    val interval = Settings.readWithVolumeKeysInterval.value + 1
                    val inverted = Settings.readWithVolumeKeysInverted.value
                    if (event.action == ACTION_DOWN && event.repeatCount % interval == 0) {
                        launch {
                            if (inverted.xor(keyCode == KEYCODE_VOLUME_UP)) {
                                movePrevious()
                            } else {
                                moveNext()
                            }
                        }
                    }
                    true
                }

                else -> false
            }
        }
        ViewCompat.addOnUnhandledKeyEventListener(view, listener)
        onDispose {
            ViewCompat.removeOnUnhandledKeyEventListener(view, listener)
        }
    }
}
