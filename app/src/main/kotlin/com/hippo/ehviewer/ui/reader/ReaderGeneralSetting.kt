package com.hippo.ehviewer.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import com.ehviewer.core.common.Res
import com.ehviewer.core.common.pref_cutout_short
import com.ehviewer.core.common.pref_double_tap_to_zoom
import com.ehviewer.core.common.pref_fullscreen
import com.ehviewer.core.common.pref_keep_screen_on
import com.ehviewer.core.common.pref_page_transitions
import com.ehviewer.core.common.pref_read_with_long_tap
import com.ehviewer.core.common.pref_reader_theme
import com.ehviewer.core.common.pref_show_page_number
import com.ehviewer.core.common.pref_show_reader_seekbar
import com.ehviewer.core.common.settings_read_reverse_controls
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.util.isAtLeastP
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReaderGeneralSetting() = Column(modifier = Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
    SpinnerChoice(
        title = stringResource(Res.string.pref_reader_theme),
        entries = stringArrayResource(id = R.array.reader_themes),
        values = stringArrayResource(id = R.array.reader_themes_values),
        field = Settings.readerTheme.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(Res.string.pref_show_page_number),
        field = Settings.showPageNumber.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(Res.string.pref_show_reader_seekbar),
        field = Settings.showReaderSeekbar.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(Res.string.pref_double_tap_to_zoom),
        field = Settings.doubleTapToZoom.asMutableState(),
    )
    val fullscreen = Settings.fullscreen.asMutableState()
    SwitchChoice(
        title = stringResource(Res.string.pref_fullscreen),
        field = fullscreen,
    )
    val view = LocalView.current
    val hasDisplayCutout = remember(view) { isAtLeastP && view.rootWindowInsets.displayCutout != null }
    if (hasDisplayCutout) {
        AnimatedVisibility(visible = fullscreen.value) {
            SwitchChoice(
                title = stringResource(Res.string.pref_cutout_short),
                field = Settings.cutoutShort.asMutableState(),
            )
        }
    }
    SwitchChoice(
        title = stringResource(Res.string.pref_keep_screen_on),
        field = Settings.keepScreenOn.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(Res.string.pref_read_with_long_tap),
        field = Settings.readerLongTapAction.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(Res.string.pref_page_transitions),
        field = Settings.pageTransitions.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(Res.string.settings_read_reverse_controls),
        field = Settings.readerReverseControls.asMutableState(),
    )
}
