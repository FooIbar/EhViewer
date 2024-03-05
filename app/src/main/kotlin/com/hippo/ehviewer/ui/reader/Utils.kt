package com.hippo.ehviewer.ui.reader

import android.app.Activity
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState

private fun Window.updateKeepScreenOn(enabled: Boolean) {
    if (enabled) {
        addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
fun Activity.ConfigureKeepScreenOn() {
    val on by Settings.keepScreenOn.collectAsState()
    DisposableEffect(on) {
        window.updateKeepScreenOn(on)
        onDispose {
            window.updateKeepScreenOn(false)
        }
    }
}
