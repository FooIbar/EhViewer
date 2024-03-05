package com.hippo.ehviewer.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState

@Composable
fun ColorFilterSetting() = Column {
    val customBrightness = Settings.customBrightness.asMutableState()
    SwitchChoice(
        title = stringResource(id = R.string.pref_custom_brightness),
        field = customBrightness,
    )
    AnimatedVisibility(visible = customBrightness.value) {
        val brightness = Settings.customBrightnessValue.asMutableState()
        SliderChoice(
            startSlot = { Icon(imageVector = Icons.Default.Brightness5, contentDescription = null) },
            endSlot = { Text(text = "${brightness.value}") },
            range = -75..100,
            field = brightness,
        )
    }
    val colorFilter = Settings.colorFilter.asMutableState()
    SwitchChoice(
        title = stringResource(id = R.string.pref_custom_color_filter),
        field = colorFilter,
    )
    AnimatedVisibility(visible = colorFilter.value) {
        var color by Settings.colorFilterValue.asMutableState()
        val rf = remember { mutableIntStateOf(color.red) }
        val r by rf
        val gf = remember { mutableIntStateOf(color.green) }
        val g by gf
        val bf = remember { mutableIntStateOf(color.blue) }
        val b by bf
        val af = remember { mutableIntStateOf(color.alpha) }
        val a by af
        color = (a shl 24) or (r shl 16) or (g shl 8) or (b)
        Column {
            SliderChoice(
                startSlot = { Text(text = "R") },
                endSlot = { Text(text = "$r") },
                range = 0..255,
                field = rf,
            )
            SliderChoice(
                startSlot = { Text(text = "G") },
                endSlot = { Text(text = "$g") },
                range = 0..255,
                field = gf,
            )
            SliderChoice(
                startSlot = { Text(text = "B") },
                endSlot = { Text(text = "$b") },
                range = 0..255,
                field = bf,
            )
            SliderChoice(
                startSlot = { Text(text = "A") },
                endSlot = { Text(text = "$a") },
                range = 0..255,
                field = af,
            )
        }
    }
    SpinnerChoice(
        title = stringResource(id = R.string.pref_color_filter_mode),
        entries = stringArrayResource(id = R.array.color_filter_modes),
        values = colorFilterValues,
        field = Settings.colorFilterMode.asMutableState(),
    )
}

private val colorFilterValues = arrayOf("0", "1", "2", "3", "4", "5")
