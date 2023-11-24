package com.hippo.ehviewer.ui.scene

import androidx.annotation.DimenRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import kotlinx.coroutines.flow.map

@Composable
fun CheckableItem(checked: Boolean, content: @Composable () -> Unit) {
    Box {
        content()
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
    fun transform(@DimenRes res: Int) = when (res) {
        0 -> long
        1 -> short
        else -> error("Unexpected value: $res")
    }
    return remember {
        Settings.detailSizeBackField.valueFlow().map { transform(it) }
    }.collectAsState(transform(Settings.detailSize))
}
