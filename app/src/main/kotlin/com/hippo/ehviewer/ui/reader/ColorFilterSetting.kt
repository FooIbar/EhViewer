package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState

@Composable
fun ColorFilterSetting() = Column {
    SwitchChoice(
        title = stringResource(id = R.string.pref_custom_brightness),
        field = Settings.customBrightness.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(id = R.string.pref_custom_color_filter),
        field = Settings.colorFilter.asMutableState(),
    )
    SpinnerChoice(
        title = stringResource(id = R.string.pref_color_filter_mode),
        entries = stringArrayResource(id = R.array.color_filter_modes),
        values = colorFilterValues,
        field = Settings.colorFilterMode.asMutableState(),
    )
}

private val colorFilterValues = arrayOf("0", "1", "2", "3", "4", "5")
