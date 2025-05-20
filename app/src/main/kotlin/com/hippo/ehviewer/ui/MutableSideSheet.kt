package com.hippo.ehviewer.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DrawerState2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf

private typealias Sheet = @Composable ColumnScope.(DrawerState2) -> Unit

val LocalSideSheetContainer = staticCompositionLocalOf<SnapshotStateList<Sheet>> { error("") }

@Composable
fun ProvideSideSheetContent(content: Sheet) {
    val container = LocalSideSheetContainer.current
    DisposableEffect(content) {
        container.add(0, content)
        onDispose { container.remove(content) }
    }
}
