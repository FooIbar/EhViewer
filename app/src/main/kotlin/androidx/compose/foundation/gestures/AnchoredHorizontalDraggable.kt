/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.HorizontalDragEvent.DragDelta
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
fun <T> Modifier.anchoredHorizontalDraggable(
    state: AnchoredDraggableState<T>,
    enableDragFromStartToEnd: Boolean = true,
    enableDragFromEndToStart: Boolean = true,
    enabled: Boolean = true,
    startDragImmediately: Boolean = state.isAnimationRunning,
    flingBehavior: FlingBehavior,
): Modifier = this then AnchoredHorizontalDraggableElement(
    state = state,
    canDrag = { (x, y) ->
        val enableDrag = if (x > 0) enableDragFromStartToEnd else enableDragFromEndToStart
        enableDrag && x.absoluteValue > y.absoluteValue * 2f
    },
    enabled = enabled,
    startDragImmediately = startDragImmediately,
    flingBehavior = flingBehavior,
)

private class AnchoredHorizontalDraggableElement<T>(
    private val state: AnchoredDraggableState<T>,
    private val canDrag: (Offset) -> Boolean,
    private val enabled: Boolean,
    private val startDragImmediately: Boolean,
    private val flingBehavior: FlingBehavior,
) : ModifierNodeElement<AnchoredHorizontalDraggableNode<T>>() {
    override fun create() = AnchoredHorizontalDraggableNode(
        state,
        canDrag,
        enabled,
        startDragImmediately,
        flingBehavior,
    )

    override fun update(node: AnchoredHorizontalDraggableNode<T>) {
        node.update(
            state,
            canDrag,
            enabled,
            startDragImmediately,
            flingBehavior,
        )
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + canDrag.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + startDragImmediately.hashCode()
        result = 31 * result + flingBehavior.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is AnchoredHorizontalDraggableElement<*>) return false

        if (state != other.state) return false
        if (canDrag != other.canDrag) return false
        if (enabled != other.enabled) return false
        if (startDragImmediately != other.startDragImmediately) return false
        if (flingBehavior != other.flingBehavior) return false

        return true
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "anchoredDraggable"
        properties["state"] = state
        properties["canDrag"] = canDrag
        properties["enabled"] = enabled
        properties["startDragImmediately"] = startDragImmediately
        properties["flingBehavior"] = flingBehavior
    }
}

private class AnchoredHorizontalDraggableNode<T>(
    private var state: AnchoredDraggableState<T>,
    private var canDrag: (Offset) -> Boolean,
    enabled: Boolean,
    private var startDragImmediately: Boolean,
    private var flingBehavior: FlingBehavior,
) : HorizontalDragGestureNode(enabled = enabled) {

    private val isReverseDirection: Boolean
        get() = requireLayoutDirection() == LayoutDirection.Rtl

    override fun canDrag(change: PointerInputChange): Boolean = canDrag(change.positionChange().reverseIfNeeded())

    override suspend fun drag(forEachDelta: suspend ((dragDelta: DragDelta) -> Unit) -> Unit) {
        state.anchoredDrag {
            forEachDelta { dragDelta ->
                dragTo(state.newOffsetForDelta(dragDelta.delta.reverseIfNeeded().x))
            }
        }
    }

    override fun onDragStarted(startedPosition: Offset) = Unit

    override fun onDragStopped(velocity: Velocity) {
        if (!isAttached) return
        coroutineScope.launch {
            fling(velocity.reverseIfNeeded().x)
        }
    }

    private suspend fun fling(velocity: Float): Float {
        var leftoverVelocity = velocity
        state.anchoredDrag {
            val scrollScope =
                object : ScrollScope {
                    override fun scrollBy(pixels: Float): Float {
                        val newOffset = state.newOffsetForDelta(pixels)
                        val consumed = newOffset - state.offset
                        dragTo(newOffset)
                        return consumed
                    }
                }
            with(flingBehavior) {
                leftoverVelocity = scrollScope.performFling(velocity)
            }
        }
        return leftoverVelocity
    }

    override fun startDragImmediately(): Boolean = startDragImmediately

    fun update(
        state: AnchoredDraggableState<T>,
        canDrag: (Offset) -> Boolean,
        enabled: Boolean,
        startDragImmediately: Boolean,
        flingBehavior: FlingBehavior,
    ) {
        this.flingBehavior = flingBehavior

        var resetPointerInputHandling = false

        if (this.state != state) {
            this.state = state
            resetPointerInputHandling = true
        }

        this.canDrag = canDrag
        this.startDragImmediately = startDragImmediately

        update(
            enabled = enabled,
            shouldResetPointerInputHandling = resetPointerInputHandling,
        )
    }

    private fun Velocity.reverseIfNeeded() = if (isReverseDirection) this * -1f else this * 1f
    private fun Offset.reverseIfNeeded() = if (isReverseDirection) this * -1f else this * 1f
}

internal fun <T> AnchoredDraggableState<T>.newOffsetForDelta(delta: Float) = ((if (offset.isNaN()) 0f else offset) + delta)
    .coerceIn(anchors.minPosition(), anchors.maxPosition())
