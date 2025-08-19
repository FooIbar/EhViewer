package androidx.compose.foundation.scrollbar

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

internal class LazyStaggeredGridScrollbarAdapter(
    private val scrollState: LazyStaggeredGridState,
) : LazyLineContentAdapter() {

    override val viewportSize: Double
        get() = with(scrollState.layoutInfo) {
            if (isVertical) viewportSize.height else viewportSize.width
        }.toDouble()

    private val isVertical by lazy {
        scrollState.layoutInfo.orientation == Orientation.Vertical
    }

    private fun LazyStaggeredGridItemInfo.mainAxisSize() = with(size) {
        if (isVertical) height else width
    }

    private fun LazyStaggeredGridItemInfo.mainAxisOffset() = with(offset) {
        if (isVertical) y else x
    }

    override fun firstVisibleLine(): VisibleLine? {
        val first = scrollState.layoutInfo.visibleItemsInfo.firstOrNull()
        return first?.run { VisibleLine(index, mainAxisOffset()) }
    }

    override fun totalLineCount() = scrollState.layoutInfo.totalItemsCount

    override fun contentPadding() = with(scrollState.layoutInfo) {
        beforeContentPadding + afterContentPadding
    }

    override suspend fun snapToLine(lineIndex: Int, scrollOffset: Int) {
        scrollState.scrollToItem(lineIndex, scrollOffset)
    }

    override suspend fun scrollBy(value: Float) {
        scrollState.scrollBy(value)
    }

    override fun averageVisibleLineSize() = with(scrollState.layoutInfo.visibleItemsInfo) {
        val first = firstOrNull() ?: return@with 0.0
        val last = last()
        (last.mainAxisOffset() + last.mainAxisSize() - first.mainAxisOffset()).toDouble() / size
    }

    override val lineSpacing get() = 0
}

@Composable
fun rememberScrollbarAdapter(
    scrollState: LazyStaggeredGridState,
): ScrollbarAdapter = remember(scrollState) {
    LazyStaggeredGridScrollbarAdapter(scrollState)
}
