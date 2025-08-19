/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.foundation.gestures.HorizontalDragEvent.DragCancelled
import androidx.compose.foundation.gestures.HorizontalDragEvent.DragDelta
import androidx.compose.foundation.gestures.HorizontalDragEvent.DragStarted
import androidx.compose.foundation.gestures.HorizontalDragEvent.DragStopped
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.sign
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A node that performs drag gesture recognition and event propagation.
 */
internal abstract class HorizontalDragGestureNode(
    enabled: Boolean,
) : DelegatingNode(),
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode {

    protected var enabled = enabled
        private set

    private var channel: Channel<HorizontalDragEvent>? = null
    private var isListeningForEvents = false

    abstract fun canDrag(change: PointerInputChange): Boolean

    /**
     * Responsible for the dragging behavior between the start and the end of the drag. It
     * continually invokes `forEachDelta` to process incoming events. In return, `forEachDelta`
     * calls `dragBy` method to process each individual delta.
     */
    abstract suspend fun drag(forEachDelta: suspend ((dragDelta: DragDelta) -> Unit) -> Unit)

    /**
     * Passes the action needed when a drag starts. This gives the ability to pass the desired
     * behavior from other nodes implementing AbstractDraggableNode
     */
    abstract fun onDragStarted(startedPosition: Offset)

    /**
     * Passes the action needed when a drag stops. This gives the ability to pass the desired
     * behavior from other nodes implementing AbstractDraggableNode
     */
    abstract fun onDragStopped(velocity: Velocity)

    /**
     * If touch slop recognition should be skipped. If this is true, this node will start
     * recognizing drag events immediately without waiting for touch slop.
     */
    abstract fun startDragImmediately(): Boolean

    private fun startListeningForEvents() {
        isListeningForEvents = true

        /**
         * To preserve the original behavior we had (before the Modifier.Node migration) we need to
         * scope the DragStopped and DragCancel methods to the node's coroutine scope instead of using
         * the one provided by the pointer input modifier, this is to ensure that even when the pointer
         * input scope is reset we will continue any coroutine scope scope that we started from these
         * methods while the pointer input scope was active.
         */
        coroutineScope.launch {
            while (isActive) {
                var event = channel?.receive()
                if (event !is DragStarted) continue
                onDragStarted(event.startPoint)
                try {
                    drag { processDelta ->
                        while (event !is DragStopped && event !is DragCancelled) {
                            (event as? DragDelta)?.let(processDelta)
                            event = channel?.receive()
                        }
                    }
                    if (event is DragStopped) {
                        onDragStopped((event as DragStopped).velocity)
                    } else if (event is DragCancelled) {
                        onDragStopped(Velocity.Zero)
                    }
                } catch (c: CancellationException) {
                    onDragStopped(Velocity.Zero)
                }
            }
        }
    }

    private var pointerInputNode: SuspendingPointerInputModifierNode? = null

    override fun onDetach() {
        isListeningForEvents = false
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (enabled && pointerInputNode == null) {
            pointerInputNode = delegate(initializePointerInputNode())
        }
        pointerInputNode?.onPointerEvent(pointerEvent, pass, bounds)
    }

    private fun initializePointerInputNode(): SuspendingPointerInputModifierNode = SuspendingPointerInputModifierNode {
        // re-create tracker when pointer input block restarts. This lazily creates the tracker
        // only when it is need.
        val velocityTracker = VelocityTracker()
        val onDragStart: (change: PointerInputChange, initialDelta: Offset) -> Unit =
            { startEvent, initialDelta ->
                if (!isListeningForEvents) {
                    if (channel == null) {
                        channel = Channel(capacity = Channel.UNLIMITED)
                    }
                    startListeningForEvents()
                }
                val xSign = sign(startEvent.position.x)
                val ySign = sign(startEvent.position.y)
                val adjustedStart = startEvent.position -
                    Offset(initialDelta.x * xSign, initialDelta.y * ySign)

                channel?.trySend(DragStarted(adjustedStart))
            }

        val onDragEnd: () -> Unit = {
            val maximumVelocity = currentValueOf(LocalViewConfiguration)
                .maximumFlingVelocity
            val velocity = velocityTracker.calculateVelocity(
                Velocity(maximumVelocity, maximumVelocity),
            )
            velocityTracker.resetTracking()
            channel?.trySend(DragStopped(velocity))
        }

        val onDragCancel: () -> Unit = {
            channel?.trySend(DragCancelled)
        }

        val shouldAwaitTouchSlop: () -> Boolean = {
            !startDragImmediately()
        }

        val onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit =
            { change, delta ->
                velocityTracker.addPointerInputChange(change)
                channel?.trySend(DragDelta(delta))
            }

        coroutineScope {
            try {
                detectHorizontalDragGestures(
                    canDrag = ::canDrag,
                    onDragStart = onDragStart,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                    shouldAwaitTouchSlop = shouldAwaitTouchSlop,
                    onDrag = onDrag,
                )
            } catch (cancellation: CancellationException) {
                channel?.trySend(DragCancelled)
                if (!isActive) throw cancellation
            }
        }
    }

    override fun onCancelPointerInput() {
        pointerInputNode?.onCancelPointerInput()
    }

    fun update(
        enabled: Boolean = this.enabled,
        shouldResetPointerInputHandling: Boolean = false,
    ) {
        var resetPointerInputHandling = shouldResetPointerInputHandling

        if (this.enabled != enabled) {
            this.enabled = enabled
            if (!enabled) {
                pointerInputNode?.let { undelegate(it) }
                pointerInputNode = null
            }
            resetPointerInputHandling = true
        }

        if (resetPointerInputHandling) {
            pointerInputNode?.resetPointerInputHandler()
        }
    }
}

internal sealed class HorizontalDragEvent {
    class DragStarted(val startPoint: Offset) : HorizontalDragEvent()
    class DragStopped(val velocity: Velocity) : HorizontalDragEvent()
    object DragCancelled : HorizontalDragEvent()
    class DragDelta(val delta: Offset) : HorizontalDragEvent()
}
