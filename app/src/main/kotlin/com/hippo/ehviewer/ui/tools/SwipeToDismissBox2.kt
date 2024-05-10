package com.hippo.ehviewer.ui.tools

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState

@Composable
fun SwipeToDismissBox2(
    state: SwipeToDismissBoxState,
    backgroundContent: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val viewConfiguration = LocalViewConfiguration.current
    val touchSlopFactor by Settings.touchSlopFactor.collectAsState { it.toFloat() }
    LocalTouchSlopProvider(touchSlopFactor) {
        SwipeToDismissBox(
            state = state,
            backgroundContent = backgroundContent,
            modifier = modifier,
            enableDismissFromStartToEnd = false,
            gesturesEnabled = gesturesEnabled,
        ) {
            CompositionLocalProvider(LocalViewConfiguration provides viewConfiguration) {
                content()
            }
        }
    }
}
