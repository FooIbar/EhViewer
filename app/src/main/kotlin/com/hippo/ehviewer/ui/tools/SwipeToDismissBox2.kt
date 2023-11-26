package com.hippo.ehviewer.ui.tools

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import com.hippo.ehviewer.Settings

@Composable
fun SwipeToDismissBox2(
    state: DismissState,
    backgroundContent: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    directions: Set<DismissDirection> = setOf(
        DismissDirection.EndToStart,
        DismissDirection.StartToEnd,
    ),
    content: @Composable RowScope.() -> Unit,
) {
    val viewConfiguration = LocalViewConfiguration.current
    LocalTouchSlopProvider(Settings.touchSlopFactor.toFloat()) {
        SwipeToDismissBox(
            state = state,
            backgroundContent = backgroundContent,
            modifier = modifier,
            directions = directions,
        ) {
            CompositionLocalProvider(LocalViewConfiguration provides viewConfiguration) {
                content()
            }
        }
    }
}
