package com.hippo.ehviewer.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState

@Composable
fun ReaderGeneralSetting() = Column {
    SpinnerChoice(
        title = stringResource(id = R.string.pref_reader_theme),
        entries = stringArrayResource(id = R.array.reader_themes),
        values = stringArrayResource(id = R.array.reader_themes_values),
        field = Settings.readerTheme.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(id = R.string.pref_show_page_number),
        field = Settings.showPageNumber.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(id = R.string.pref_show_reader_seekbar),
        field = Settings.showReaderSeekbar.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(id = R.string.pref_double_tap_to_zoom),
        field = Settings.doubleTapToZoom.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(id = R.string.pref_fullscreen),
        field = Settings.fullscreen.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(id = R.string.pref_cutout_short),
        field = Settings.cutoutShort.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(id = R.string.pref_keep_screen_on),
        field = Settings.keepScreenOn.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(id = R.string.pref_read_with_long_tap),
        field = Settings.readerLongTapAction.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(id = R.string.pref_page_transitions),
        field = Settings.pageTransitions.asMutableState(),
    )
    val volume = Settings.readWithVolumeKeys.asMutableState()
    SwitchChoice(
        title = stringResource(id = R.string.settings_read_volume_page),
        field = volume,
    )
    AnimatedVisibility(visible = volume.value) {
        Column {
            SliderChoice(
                startSlot = { Text(text = stringResource(id = R.string.settings_read_volume_page_fast)) },
                endSlot = { Text(text = stringResource(id = R.string.settings_read_volume_page_slow)) },
                range = 0..9,
                field = Settings.readWithVolumeKeysInterval.asMutableState(),
            )
            SwitchChoice(
                title = stringResource(id = R.string.settings_read_reverse_volume),
                field = Settings.readWithVolumeKeysInverted.asMutableState(),
            )
        }
    }
}
