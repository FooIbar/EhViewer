package com.ehviewer.core.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.IconToggleButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun FilledTertiaryIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: IconButtonShapes = IconButtonDefaults.shapes(),
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) = FilledIconButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    shapes = shapes,
    colors = colors,
    interactionSource = interactionSource,
    content = content,
)

@Composable
fun FilledTertiaryIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: IconToggleButtonShapes = IconButtonDefaults.toggleableShapes(),
    colors: IconToggleButtonColors = IconButtonDefaults.filledTonalIconToggleButtonColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) = FilledTonalIconToggleButton(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier,
    enabled = enabled,
    shapes = shapes,
    colors = colors,
    interactionSource = interactionSource,
    content = content,
)
