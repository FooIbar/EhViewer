package com.hippo.ehviewer.ui.screen

import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

@Composable
fun CheckableItem(
    checked: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (MutableInteractionSource) -> Unit,
) {
    val src = controlledInteractionSource(enabled = checked)
    Box(modifier) {
        content(src)
        if (checked) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
@Stable
fun collectDetailSizeAsState(): State<Dp> {
    val long = dimensionResource(R.dimen.gallery_list_column_width_long)
    val short = dimensionResource(R.dimen.gallery_list_column_width_short)
    return Settings.detailSize.collectAsState {
        when (it) {
            0 -> long
            1 -> short
            else -> error("Unexpected value: $it")
        }
    }
}

@Composable
@Stable
fun collectListThumbSizeAsState(): State<Dp> {
    val density = LocalDensity.current
    return Settings.listThumbSize.collectAsState {
        with(density) {
            it.toDp() * 9
        }
    }
}

@Composable
private fun controlledInteractionSource(enabled: Boolean): MutableInteractionSource {
    val update by rememberUpdatedState(newValue = enabled)
    return remember {
        val impl = MutableInteractionSource()
        val controlled = snapshotFlow { update }.map { enable ->
            if (enable) enter else exit
        }
        object : MutableInteractionSource by impl {
            override val interactions = merge(controlled, impl.interactions)
        }
    }
}

private val enter = HoverInteraction.Enter()
private val exit = HoverInteraction.Exit(enter)
