/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.scrollbar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Defines how to scroll the scrollable component and how to display a scrollbar for it.
 *
 * The values of this interface are typically in pixels, but do not have to be.
 * It's possible to create an adapter with any scroll range of `Double` values.
 */
interface ScrollbarAdapter {

    // We use `Double` values here in order to allow scrolling both very large (think LazyList with
    // millions of items) and very small (think something whose natural coordinates are less than 1)
    // content.

    /**
     * Scroll offset of the content inside the scrollable component.
     *
     * For example, a value of `100` could mean the content is scrolled by 100 pixels from the
     * start.
     */
    val scrollOffset: Double

    /**
     * The size of the scrollable content, on the scrollable axis.
     */
    val contentSize: Double

    /**
     * The size of the viewport, on the scrollable axis.
     */
    val viewportSize: Double

    /**
     * Instantly jump to [scrollOffset].
     *
     * @param scrollOffset target offset to jump to, value will be coerced to the valid
     * scroll range.
     */
    suspend fun scrollTo(scrollOffset: Double)
}

/**
 * The maximum scroll offset of the scrollable content.
 */
val ScrollbarAdapter.maxScrollOffset: Double
    get() = (contentSize - viewportSize).coerceAtLeast(0.0)

internal class ScrollableScrollbarAdapter(
    private val scrollState: ScrollState,
) : ScrollbarAdapter {

    override val scrollOffset: Double get() = scrollState.value.toDouble()

    override suspend fun scrollTo(scrollOffset: Double) {
        scrollState.scrollTo(scrollOffset.roundToInt())
    }

    override val contentSize: Double
        // This isn't strictly correct, as the actual content can be smaller
        // than the viewport when scrollState.maxValue is 0, but the scrollbar
        // doesn't really care as long as contentSize <= viewportSize; it's
        // just not showing itself
        get() = scrollState.maxValue + viewportSize

    override val viewportSize: Double
        get() = scrollState.viewportSize.toDouble()
}

/**
 * Base class for [LazyListScrollbarAdapter] and [LazyGridScrollbarAdapter],
 * and in the future maybe other lazy widgets that lay out their content in lines.
 */
internal abstract class LazyLineContentAdapter : ScrollbarAdapter {

    // Implement the adapter in terms of "lines", which means either rows,
    // (for a vertically scrollable widget) or columns (for a horizontally
    // scrollable one).
    // For LazyList this translates directly to items; for LazyGrid, it
    // translates to rows/columns of items.

    class VisibleLine(
        val index: Int,
        val offset: Int,
    )

    /**
     * Return the first visible line, if any.
     */
    protected abstract fun firstVisibleLine(): VisibleLine?

    /**
     * Return the total number of lines.
     */
    protected abstract fun totalLineCount(): Int

    /**
     * The sum of content padding (before+after) on the scrollable axis.
     */
    protected abstract fun contentPadding(): Int

    /**
     * Scroll immediately to the given line, and offset it by [scrollOffset] pixels.
     */
    protected abstract suspend fun snapToLine(lineIndex: Int, scrollOffset: Int)

    /**
     * Scroll from the current position by the given amount of pixels.
     */
    protected abstract suspend fun scrollBy(value: Float)

    /**
     * Return the average size (on the scrollable axis) of the visible lines.
     */
    protected abstract fun averageVisibleLineSize(): Double

    /**
     * The spacing between lines.
     */
    protected abstract val lineSpacing: Int

    private val averageVisibleLineSize by derivedStateOf {
        if (totalLineCount() == 0) {
            0.0
        } else {
            averageVisibleLineSize()
        }
    }

    private val averageVisibleLineSizeWithSpacing get() = averageVisibleLineSize + lineSpacing

    override val scrollOffset: Double
        get() {
            val firstVisibleLine = firstVisibleLine()
            return if (firstVisibleLine == null) {
                0.0
            } else {
                firstVisibleLine.index * averageVisibleLineSizeWithSpacing - firstVisibleLine.offset
            }
        }

    override val contentSize: Double
        get() {
            val totalLineCount = totalLineCount()
            return averageVisibleLineSize * totalLineCount +
                lineSpacing * (totalLineCount - 1).coerceAtLeast(0) +
                contentPadding()
        }

    override suspend fun scrollTo(scrollOffset: Double) {
        val distance = scrollOffset - this@LazyLineContentAdapter.scrollOffset

        // if we scroll less than viewport we need to use scrollBy function to avoid
        // undesirable scroll jumps (when an item size is different)
        //
        // if we scroll more than viewport we should immediately jump to this position
        // without recreating all items between the current and the new position
        if (abs(distance) <= viewportSize) {
            scrollBy(distance.toFloat())
        } else {
            snapTo(scrollOffset)
        }
    }

    private suspend fun snapTo(scrollOffset: Double) {
        val scrollOffsetCoerced = scrollOffset.coerceIn(0.0, maxScrollOffset)

        val index = (scrollOffsetCoerced / averageVisibleLineSizeWithSpacing)
            .toInt()
            .coerceAtLeast(0)
            .coerceAtMost(totalLineCount() - 1)

        val offset = (scrollOffsetCoerced - index * averageVisibleLineSizeWithSpacing)
            .toInt()
            .coerceAtLeast(0)

        snapToLine(lineIndex = index, scrollOffset = offset)
    }
}

internal class LazyListScrollbarAdapter(
    private val scrollState: LazyListState,
) : LazyLineContentAdapter() {

    override val viewportSize: Double
        get() = with(scrollState.layoutInfo) {
            if (orientation == Orientation.Vertical) {
                viewportSize.height
            } else {
                viewportSize.width
            }
        }.toDouble()

    /**
     * A heuristic that tries to ignore the "currently stickied" header because it breaks the other
     * computations in this adapter:
     * - The currently stickied header always appears in the list of visible items, with its
     *   regular index. This makes [firstVisibleLine] always return its index, even if the list has
     *   been scrolled far beyond it.
     * - [averageVisibleLineSize] calculates the average size in O(1) by assuming that items don't
     *   overlap, and the stickied item breaks this assumption.
     *
     * Attempts to return the index into `visibleItemsInfo` of the first non-currently-stickied (it
     * could be sticky, but not stickied to the top of the list right now) item, if there is one.
     *
     * Note that this heuristic breaks down if the sticky header covers the entire list, so that
     * it's the only visible item for some portion of the scroll range. But there's currently no
     * known better way to solve it, and it's a relatively unusual case.
     */
    private fun firstFloatingVisibleItemIndex() = with(scrollState.layoutInfo.visibleItemsInfo) {
        when (size) {
            0 -> null
            1 -> 0
            else -> {
                val first = this[0]
                val second = this[1]
                // If either the indices or the offsets aren't continuous, then the first item is
                // sticky, so we return 1
                if ((first.index < second.index - 1) ||
                    (first.offset + first.size + lineSpacing > second.offset)
                ) {
                    1
                } else {
                    0
                }
            }
        }
    }

    override fun firstVisibleLine(): VisibleLine? {
        val firstFloatingVisibleIndex = firstFloatingVisibleItemIndex() ?: return null
        val firstFloatingItem = scrollState.layoutInfo.visibleItemsInfo[firstFloatingVisibleIndex]
        return VisibleLine(
            index = firstFloatingItem.index,
            offset = firstFloatingItem.offset,
        )
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
        val firstFloatingIndex = firstFloatingVisibleItemIndex() ?: return@with 0.0
        val first = this[firstFloatingIndex]
        val last = last()
        val count = size - firstFloatingIndex
        (last.offset + last.size - first.offset - (count - 1) * lineSpacing).toDouble() / count
    }

    override val lineSpacing get() = scrollState.layoutInfo.mainAxisItemSpacing
}

internal class LazyGridScrollbarAdapter(
    private val scrollState: LazyGridState,
) : LazyLineContentAdapter() {

    override val viewportSize: Double
        get() = with(scrollState.layoutInfo) {
            if (isVertical) {
                viewportSize.height
            } else {
                viewportSize.width
            }
        }.toDouble()

    private val isVertical by lazy {
        scrollState.layoutInfo.orientation == Orientation.Vertical
    }

    private val unknownLine = with(LazyGridItemInfo) {
        if (isVertical) UnknownRow else UnknownColumn
    }

    private fun LazyGridItemInfo.line() = if (isVertical) row else column

    private fun LazyGridItemInfo.mainAxisSize() = with(size) {
        if (isVertical) height else width
    }

    private fun LazyGridItemInfo.mainAxisOffset() = with(offset) {
        if (isVertical) y else x
    }

    private fun slotsPerLine(): Int = scrollState.layoutInfo.visibleItemsInfo
        .groupingBy { it.line() }.eachCount().maxOfOrNull { it.value } ?: 1

    private fun lineOfIndex(index: Int) = index / slotsPerLine()

    private fun indexOfFirstInLine(line: Int) = line * slotsPerLine()

    override fun firstVisibleLine(): VisibleLine? = scrollState.layoutInfo.visibleItemsInfo
        .firstOrNull { it.line() != unknownLine } // Skip exiting items
        ?.let { firstVisibleItem ->
            VisibleLine(
                index = firstVisibleItem.line(),
                offset = firstVisibleItem.mainAxisOffset(),
            )
        }

    override fun totalLineCount(): Int {
        val itemCount = scrollState.layoutInfo.totalItemsCount
        return if (itemCount == 0) {
            0
        } else {
            lineOfIndex(itemCount - 1) + 1
        }
    }

    override fun contentPadding() = with(scrollState.layoutInfo) {
        beforeContentPadding + afterContentPadding
    }

    override suspend fun snapToLine(lineIndex: Int, scrollOffset: Int) {
        scrollState.scrollToItem(
            index = indexOfFirstInLine(lineIndex),
            scrollOffset = scrollOffset,
        )
    }

    override suspend fun scrollBy(value: Float) {
        scrollState.scrollBy(value)
    }

    override fun averageVisibleLineSize(): Double {
        val visibleItemsInfo = // Non-exiting visible items
            scrollState.layoutInfo.visibleItemsInfo.filter { it.line() != unknownLine }
        if (visibleItemsInfo.isEmpty()) {
            return 0.0
        }

        // Compute the size of the last line
        val last = visibleItemsInfo.last()
        val lastLine = last.line()
        val lastLineSize = visibleItemsInfo
            .asReversed()
            .asSequence()
            .takeWhile { it.line() == lastLine }
            .maxOf { it.mainAxisSize() }

        val first = visibleItemsInfo.first()
        val lineCount = lastLine - first.line() + 1
        val lineSpacingSum = (lineCount - 1) * lineSpacing
        return (
            last.mainAxisOffset() + lastLineSize - first.mainAxisOffset() - lineSpacingSum
            ).toDouble() / lineCount
    }

    override val lineSpacing get() = scrollState.layoutInfo.mainAxisItemSpacing
}

internal class SliderAdapter(
    private val adapter: ScrollbarAdapter,
    private val trackSize: Int,
    private val minHeight: Float,
    private val reverseLayout: Boolean,
    private val isVertical: Boolean,
    private val coroutineScope: CoroutineScope,
) {

    private val contentSize get() = adapter.contentSize
    private val visiblePart: Double
        get() {
            val contentSize = contentSize
            return if (contentSize == 0.0) {
                1.0
            } else {
                (adapter.viewportSize / contentSize).coerceAtMost(1.0)
            }
        }

    val thumbSize
        get() = (trackSize * visiblePart).coerceAtLeast(minHeight.toDouble())

    private val scrollScale: Double
        get() {
            val extraScrollbarSpace = trackSize - thumbSize
            val extraContentSpace = adapter.maxScrollOffset // == contentSize - viewportSize
            return if (extraContentSpace == 0.0) 1.0 else extraScrollbarSpace / extraContentSpace
        }

    private val rawPosition: Double
        get() = scrollScale * adapter.scrollOffset

    val position: Double
        get() = if (reverseLayout) trackSize - thumbSize - rawPosition else rawPosition

    // How much of the current drag was ignored because we've reached the end of the scrollbar area
    private var unscrolledDragDistance = 0.0

    /** Called when the thumb dragging starts */
    fun onDragStarted() {
        unscrolledDragDistance = 0.0
    }

    private suspend fun setPosition(value: Double) {
        val rawPosition = if (reverseLayout) {
            trackSize - thumbSize - value
        } else {
            value
        }
        adapter.scrollTo(rawPosition / scrollScale)
    }

    private val dragMutex = Mutex()

    /** Called on every movement while dragging the thumb */
    fun onDragDelta(offset: Offset) {
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            // Mutex is used to ensure that all earlier drag deltas were applied
            // before calculating a new raw position
            dragMutex.withLock {
                val dragDelta = if (isVertical) offset.y else offset.x
                val maxScrollPosition = adapter.maxScrollOffset * scrollScale
                val currentPosition = position
                val targetPosition =
                    (currentPosition + dragDelta + unscrolledDragDistance).coerceIn(
                        0.0,
                        maxScrollPosition,
                    )
                val sliderDelta = targetPosition - currentPosition

                // Have to add to position for smooth content scroll if the items are of different size
                val newPos = position + sliderDelta
                setPosition(newPos)
                unscrolledDragDistance += dragDelta - sliderDelta
            }
        }
    }
}
