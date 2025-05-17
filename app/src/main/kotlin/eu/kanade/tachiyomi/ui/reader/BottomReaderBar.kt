package eu.kanade.tachiyomi.ui.reader

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.asMutableState
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.filled.Crop
import com.hippo.ehviewer.icons.filled.CropOff
import eu.kanade.tachiyomi.ui.reader.setting.OrientationType
import eu.kanade.tachiyomi.ui.reader.setting.PreferenceType
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType

@Composable
fun BottomReaderBar(onClickSettings: () -> Unit) = FlexibleBottomAppBar(
    containerColor = toolbarColor,
    contentPadding = PaddingValues.Zero,
    horizontalArrangement = Arrangement.SpaceEvenly,
) {
    val readingMode by Settings.readingMode.collectAsState { ReadingModeType.fromPreference(it) }
    DropdownIconButton(
        label = stringResource(R.string.viewer),
        menuItems = ReadingModeType.entries,
        selectedItem = readingMode,
        onSelectedItemChange = {
            Settings.readingMode.value = it.flagValue
        },
        minMenuWidth = 192.dp,
    )
    val orientationMode by Settings.orientationMode.collectAsState { OrientationType.fromPreference(it) }
    DropdownIconButton(
        label = stringResource(R.string.pref_rotation_type),
        menuItems = OrientationType.entries,
        selectedItem = orientationMode,
        onSelectedItemChange = {
            Settings.orientationMode.value = it.flagValue
        },
        minMenuWidth = 192.dp,
    )
    var cropBorder by Settings.cropBorder.asMutableState()
    ActionButton(
        onClick = { cropBorder = !cropBorder },
        imageVector = if (cropBorder) EhIcons.Default.Crop else EhIcons.Default.CropOff,
        contentDescription = stringResource(R.string.pref_crop_borders),
    )
    ActionButton(
        onClick = onClickSettings,
        imageVector = Icons.Outlined.Settings,
        contentDescription = stringResource(R.string.action_settings),
    )
}

val toolbarColor
    @Composable get() = BottomAppBarDefaults.containerColor.copy(alpha = if (isSystemInDarkTheme()) 0.9f else 0.95f)

@Composable
private fun DropdownIconButton(
    label: String,
    menuItems: List<PreferenceType>,
    selectedItem: PreferenceType,
    onSelectedItemChange: (PreferenceType) -> Unit,
    modifier: Modifier = Modifier,
    minMenuWidth: Dp = Dp.Unspecified,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        ActionButton(
            onClick = {},
            imageVector = selectedItem.icon,
            contentDescription = label,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = minMenuWidth),
            matchAnchorWidth = false,
        ) {
            menuItems.forEach {
                DropdownMenuItem(
                    text = { Text(stringResource(it.stringRes)) },
                    onClick = {
                        expanded = false
                        onSelectedItemChange(it)
                    },
                    leadingIcon = {
                        if (selectedItem == it) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
        shape = IconButtonDefaults.mediumRoundShape,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
        )
    }
}
