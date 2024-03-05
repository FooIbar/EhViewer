package com.hippo.ehviewer.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
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
import moe.tarsin.kt.unreachable

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
        Column {
            SliderChoice(
                startSlot = { Text(text = "R") },
                endSlot = { Text(text = "${color.red}") },
                range = 0..255,
                field = remember {
                    object : MutableState<Int> {
                        override var value: Int
                            get() = color.red
                            set(value) {
                                color = color.convert(value, RED_MASK, 16)
                            }
                        override fun component1() = unreachable()
                        override fun component2() = unreachable()
                    }
                },
            )
            SliderChoice(
                startSlot = { Text(text = "G") },
                endSlot = { Text(text = "${color.green}") },
                range = 0..255,
                field = remember {
                    object : MutableState<Int> {
                        override var value: Int
                            get() = color.green
                            set(value) {
                                color = color.convert(value, GREEN_MASK, 8)
                            }
                        override fun component1() = unreachable()
                        override fun component2() = unreachable()
                    }
                },
            )
            SliderChoice(
                startSlot = { Text(text = "B") },
                endSlot = { Text(text = "${color.blue}") },
                range = 0..255,
                field = remember {
                    object : MutableState<Int> {
                        override var value: Int
                            get() = color.blue
                            set(value) {
                                color = color.convert(value, BLUE_MASK, 0)
                            }
                        override fun component1() = unreachable()
                        override fun component2() = unreachable()
                    }
                },
            )
            SliderChoice(
                startSlot = { Text(text = "A") },
                endSlot = { Text(text = "${color.alpha}") },
                range = 0..255,
                field = remember {
                    object : MutableState<Int> {
                        override var value: Int
                            get() = color.alpha
                            set(value) {
                                color = color.convert(value, ALPHA_MASK, 24)
                            }
                        override fun component1() = unreachable()
                        override fun component2() = unreachable()
                    }
                },
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

private const val ALPHA_MASK: Long = 0xFF000000
private const val RED_MASK: Long = 0x00FF0000
private const val GREEN_MASK: Long = 0x0000FF00
private const val BLUE_MASK: Long = 0x000000FF

private fun Int.convert(color: Int, mask: Long, bitShift: Int) = (color shl bitShift) or (this and mask.inv().toInt())

private val colorFilterValues = arrayOf("0", "1", "2", "3", "4", "5")
