package com.hippo.ehviewer.ui.reader

import android.app.Activity
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.hippo.ehviewer.Settings
import eu.kanade.tachiyomi.ui.reader.setting.OrientationType
import kotlinx.coroutines.flow.onCompletion

fun Window.updateKeepScreenOn(enabled: Boolean) {
    if (enabled) {
        addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
fun Activity.ConfigureKeepScreenOn() {
    LaunchedEffect(Unit) {
        Settings.keepScreenOn.valueFlow()
            .onCompletion { window.updateKeepScreenOn(false) }
            .collect { window.updateKeepScreenOn(it) }
    }
}

/**
 * Forces the user preferred [orientation] on the activity.
 */
fun Activity.setOrientation(orientation: Int) {
    val newOrientation = OrientationType.fromPreference(orientation)
    if (newOrientation.flag != requestedOrientation) {
        requestedOrientation = newOrientation.flag
    }
}

/**
 * Sets the brightness of the screen. Range is [-75, 100].
 * From -75 to -1 a semi-transparent black view is overlaid with the minimum brightness.
 * From 1 to 100 it sets that value as brightness.
 * 0 sets system brightness and hides the overlay.
 */
fun Activity.setCustomBrightnessValue(value: Int) {
    val readerBrightness = when {
        value > 0 -> value / 100f
        value < 0 -> 0.01f
        else -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }

    window.attributes = window.attributes.apply { screenBrightness = readerBrightness }
}
