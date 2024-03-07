package com.hippo.ehviewer.ui.reader

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.collectAsState
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType

@Composable
fun ReadModeSetting() = Column {
    SpinnerChoice(
        title = stringResource(id = R.string.pref_category_reading_mode),
        entries = stringArrayResource(id = R.array.viewers_selector),
        values = viewerValues,
        field = Settings.readingMode.asMutableState(),
    )
    SpinnerChoice(
        title = stringResource(id = R.string.rotation_type),
        entries = stringArrayResource(id = R.array.rotation_type),
        values = rotationValues,
        field = Settings.orientationMode.asMutableState(),
    )
    Spacer(modifier = Modifier.size(24.dp))
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
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun WebtoonSetting() = Column {
    Text(
        text = stringResource(id = R.string.webtoon_viewer),
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

private val viewerValues = arrayOf("0", "1", "2", "3", "4", "5")
private val rotationValues = arrayOf("0", "1", "2", "3", "4", "5", "6")
