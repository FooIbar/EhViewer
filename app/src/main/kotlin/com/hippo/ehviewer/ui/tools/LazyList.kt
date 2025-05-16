package com.hippo.ehviewer.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.scrollbar.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset

@Composable
fun FastScrollLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues.Zero,
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    Layout({
        LazyColumn(
            modifier = modifier,
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            userScrollEnabled = userScrollEnabled,
            content = content,
        )
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(state),
            isScrollInProgress = state.isScrollInProgress,
            contentPadding = contentPadding,
        )
    }) { measurables, constraints ->
        val (column, scrollbar) = measurables.map { it.measure(constraints) }
        layout(column.width, column.height) {
            column.place(IntOffset.Zero)
            scrollbar.placeRelative(column.width - scrollbar.width, 0)
        }
    }
}
