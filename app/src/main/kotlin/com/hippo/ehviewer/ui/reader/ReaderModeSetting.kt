package com.hippo.ehviewer.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ehviewer.core.i18n.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.collectAsState
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType

@Composable
fun ReaderModeSetting() = Column(modifier = Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
    SpinnerChoice(
        title = stringResource(id = R.string.pref_category_reading_mode),
        entries = stringArrayResource(id = com.hippo.ehviewer.R.array.viewers_selector),
        values = arrayOf("0", "1", "2", "3", "4", "5"),
        field = Settings.readingMode.asMutableState(),
    )
    SpinnerChoice(
        title = stringResource(id = R.string.rotation_type),
        entries = stringArrayResource(id = com.hippo.ehviewer.R.array.rotation_type),
        values = arrayOf("0", "8", "16", "24", "32", "40", "48"),
        field = Settings.orientationMode.asMutableState(),
    )
    Spacer(modifier = Modifier.size(16.dp))
    val isPager by Settings.readingMode.collectAsState { value ->
        ReadingModeType.isPagerType(value)
    }
    Crossfade(targetState = isPager, label = "Pager") { pager ->
        if (pager) {
            PagerSetting()
        } else {
            WebtoonSetting()
        }
    }
}

@Composable
private fun PagerSetting() = Column {
    Text(
        text = stringResource(id = R.string.pager_viewer),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val pagerNav = Settings.readerPagerNav.asMutableState()
    SpinnerChoice(
        title = stringResource(id = R.string.pref_viewer_nav),
        entries = stringArrayResource(id = com.hippo.ehviewer.R.array.pager_nav),
        values = arrayOf("0", "1", "2", "3", "4", "5"),
        field = pagerNav,
    )
    AnimatedVisibility(visible = pagerNav.value != 5) {
        Column {
            SpinnerChoice(
                title = stringResource(id = R.string.pref_read_with_tapping_inverted),
                entries = stringArrayResource(id = com.hippo.ehviewer.R.array.invert_tapping_mode),
                values = arrayOf("0", "1", "2", "3"),
                field = Settings.readerPagerNavInverted.asMutableState(),
            )
            SwitchChoice(
                title = stringResource(id = R.string.pref_navigate_pan),
                field = Settings.navigateToPan.asMutableState(),
            )
        }
    }
    val scaleType = Settings.imageScaleType.asMutableState()
    SpinnerChoice(
        title = stringResource(id = R.string.pref_image_scale_type),
        entries = stringArrayResource(id = com.hippo.ehviewer.R.array.image_scale_type),
        values = arrayOf("1", "2", "3", "4", "5", "6"),
        field = scaleType,
    )
    AnimatedVisibility(visible = scaleType.value == 1) {
        SwitchChoice(
            title = stringResource(id = R.string.pref_landscape_zoom),
            field = Settings.landscapeZoom.asMutableState(),
        )
    }
    SpinnerChoice(
        title = stringResource(id = R.string.pref_zoom_start),
        entries = stringArrayResource(id = com.hippo.ehviewer.R.array.zoom_start),
        values = arrayOf("1", "2", "3", "4"),
        field = Settings.zoomStart.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(id = R.string.pref_crop_borders),
        field = Settings.cropBorder.asMutableState(),
    )
}

@Composable
private fun WebtoonSetting() = Column {
    Text(
        text = stringResource(id = R.string.webtoon_viewer),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val webtoonNav = Settings.readerWebtoonNav.asMutableState()
    SpinnerChoice(
        title = stringResource(id = R.string.pref_viewer_nav),
        entries = stringArrayResource(id = com.hippo.ehviewer.R.array.webtoon_nav),
        values = arrayOf("0", "1", "2", "3", "4", "5"),
        field = webtoonNav,
    )
    AnimatedVisibility(visible = webtoonNav.value != 5) {
        SpinnerChoice(
            title = stringResource(id = R.string.pref_read_with_tapping_inverted),
            entries = stringArrayResource(id = com.hippo.ehviewer.R.array.invert_tapping_mode),
            values = arrayOf("0", "1", "2", "3"),
            field = Settings.readerWebtoonNavInverted.asMutableState(),
        )
    }
    SpinnerChoice(
        title = stringResource(id = R.string.pref_webtoon_side_padding),
        entries = stringArrayResource(id = com.hippo.ehviewer.R.array.webtoon_side_padding),
        values = stringArrayResource(id = com.hippo.ehviewer.R.array.webtoon_side_padding_values),
        field = Settings.webtoonSidePadding.asMutableState(),
    )
    SwitchChoice(
        title = stringResource(id = R.string.pref_crop_borders),
        field = Settings.cropBorder.asMutableState(),
    )
}
