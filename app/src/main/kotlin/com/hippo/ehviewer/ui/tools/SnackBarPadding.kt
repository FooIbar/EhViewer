package com.hippo.ehviewer.ui.tools

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.ui.LocalSnackBarFabPadding

@Stable
@ReadOnlyComposable
private fun State<Dp>.asBottomPaddingValues() = object : PaddingValues {
    override fun calculateBottomPadding() = value
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) = 0.dp
    override fun calculateRightPadding(layoutDirection: LayoutDirection) = 0.dp
    override fun calculateTopPadding() = 0.dp
}

@ReadOnlyComposable
@Composable
@Stable
fun Modifier.snackBarPadding(): Modifier {
    val snackBarPadding = LocalSnackBarFabPadding.current.asBottomPaddingValues()
    return padding(snackBarPadding)
}
