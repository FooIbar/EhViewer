package eu.kanade.tachiyomi.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.filled.Crop
import com.hippo.ehviewer.icons.filled.CropOff
import eu.kanade.tachiyomi.ui.reader.setting.OrientationType
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType

@Composable
fun BottomReaderBar(onClickSettings: () -> Unit) {
    // Match with toolbar background color set in ReaderActivity
    val backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp).copy(alpha = if (isSystemInDarkTheme()) 0.9f else 0.95f)

    val readingMode by Settings.readingMode.collectAsState { ReadingModeType.fromPreference(it) }
    var readingModeExpanded by remember { mutableStateOf(false) }
    Box {
        DropdownMenu(
            readingModeExpanded,
            onDismissRequest = { readingModeExpanded = false },
        ) {
            ReadingModeType.entries.forEach {
                DropdownMenuItem(
                    text = { Text(stringResource(it.stringRes)) },
                    onClick = {
                        Settings.readingMode.value = it.flagValue
                        readingModeExpanded = false
                    },
                    modifier = Modifier.width(192.dp),
                    leadingIcon = {
                        if (readingMode == it) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        }
                    },
                )
            }
        }
    }

    val orientationMode by Settings.orientationMode.collectAsState { OrientationType.fromPreference(it) }
    var orientationModeExpanded by remember { mutableStateOf(false) }
    Box {
        DropdownMenu(
            orientationModeExpanded,
            onDismissRequest = { orientationModeExpanded = false },
            offset = DpOffset(320.dp, 0.dp),
        ) {
            OrientationType.entries.forEach {
                DropdownMenuItem(
                    text = { Text(stringResource(it.stringRes)) },
                    onClick = {
                        Settings.orientationMode.value = it.flagValue
                        orientationModeExpanded = false
                    },
                    modifier = Modifier.width(192.dp),
                    leadingIcon = {
                        if (orientationMode == it) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        }
                    },
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().background(backgroundColor).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { readingModeExpanded = true }) {
            Icon(
                painter = painterResource(readingMode.iconRes),
                contentDescription = stringResource(R.string.viewer),
            )
        }
        IconButton(onClick = { orientationModeExpanded = true }) {
            Icon(
                painter = painterResource(orientationMode.iconRes),
                contentDescription = stringResource(R.string.pref_rotation_type),
            )
        }
        var cropBorder by Settings.cropBorder.asMutableState()
        IconButton(onClick = { cropBorder = !cropBorder }) {
            Icon(
                imageVector = if (cropBorder) EhIcons.Default.Crop else EhIcons.Default.CropOff,
                contentDescription = stringResource(R.string.pref_crop_borders),
            )
        }
        IconButton(onClick = onClickSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.action_settings),
            )
        }
    }
}
