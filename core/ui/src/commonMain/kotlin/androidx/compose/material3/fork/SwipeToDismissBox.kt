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

package androidx.compose.material3.fork

import androidx.annotation.FloatRange
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredHorizontalDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException

/**
 * State of the [SwipeToDismissBox] composable.
 *
 * @param initialValue The initial value of the state.
 * @param positionalThreshold The positional threshold to be used when calculating the target
 *   state while a swipe is in progress and when settling after the swipe ends. This is the
 *   distance from the start of a transition. It will be, depending on the direction of the
 *   interaction, added or subtracted from/to the origin offset. It should always be a positive
 *   value.
 */
class SwipeToDismissBoxState(
    initialValue: SwipeToDismissBoxValue,
    internal val positionalThreshold: (totalDistance: Float) -> Float,
) {
    internal val anchoredDraggableState = AnchoredDraggableState(initialValue)

    internal val offset: Float
        get() = anchoredDraggableState.offset

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    /** The current state value of the [SwipeToDismissBoxState]. */
    val currentValue: SwipeToDismissBoxValue
        get() = anchoredDraggableState.currentValue

    /**
     * The target state. This is the closest state to the current offset (taking into account
     * positional thresholds). If no interactions like animations or drags are in progress, this
     * will be the current state.
     */
    val targetValue: SwipeToDismissBoxValue
        get() = anchoredDraggableState.targetValue

    /**
     * The value the [SwipeToDismissBoxState] is currently settled at. When progressing through
     * multiple anchors, e.g. A -> B -> C, settledValue will stay the same until settled at an
     * anchor, while currentValue will update to the closest anchor.
     */
    val settledValue: SwipeToDismissBoxValue
        get() = anchoredDraggableState.settledValue

    /**
     * The fraction of the progress going from currentValue to targetValue, within [0f..1f] bounds.
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val progress: Float
        get() = anchoredDraggableState.progress(anchoredDraggableState.settledValue, anchoredDraggableState.targetValue)

    /**
     * The direction (if any) in which the composable has been or is being dismissed.
     *
     * Use this to change the background of the [SwipeToDismissBox] if you want different actions on
     * each side.
     */
    val dismissDirection: SwipeToDismissBoxValue
        get() = when {
            offset == 0f || offset.isNaN() -> SwipeToDismissBoxValue.Settled
            offset > 0f -> SwipeToDismissBoxValue.StartToEnd
            else -> SwipeToDismissBoxValue.EndToStart
        }

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value
     */
    suspend fun snapTo(targetValue: SwipeToDismissBoxValue) {
        anchoredDraggableState.snapTo(targetValue)
    }

    /**
     * Reset the component to the default position with animation and suspend until it if fully
     * reset or animation has been cancelled. This method will throw [CancellationException] if the
     * animation is interrupted
     *
     * @return the reason the reset animation ended
     */
    suspend fun reset() = anchoredDraggableState.animateTo(targetValue = SwipeToDismissBoxValue.Settled)

    /**
     * Dismiss the component in the given [direction], with an animation and suspend. This method
     * will throw [CancellationException] if the animation is interrupted
     *
     * @param direction The dismiss direction.
     */
    suspend fun dismiss(direction: SwipeToDismissBoxValue) {
        anchoredDraggableState.animateTo(targetValue = direction)
    }

    companion object {

        /** The default [Saver] implementation for [SwipeToDismissBoxState]. */
        fun Saver(positionalThreshold: (totalDistance: Float) -> Float) = Saver<SwipeToDismissBoxState, SwipeToDismissBoxValue>(
            save = { it.currentValue },
            restore = { SwipeToDismissBoxState(it, positionalThreshold) },
        )
    }
}

/**
 * Create and [remember] a [SwipeToDismissBoxState].
 *
 * @param initialValue The initial value of the state.
 * @param positionalThreshold The positional threshold to be used when calculating the target state
 *   while a swipe is in progress and when settling after the swipe ends. This is the distance from
 *   the start of a transition. It will be, depending on the direction of the interaction, added or
 *   subtracted from/to the origin offset. It should always be a positive value.
 */
@Composable
fun rememberSwipeToDismissBoxState(
    initialValue: SwipeToDismissBoxValue = SwipeToDismissBoxValue.Settled,
    positionalThreshold: (totalDistance: Float) -> Float = SwipeToDismissBoxDefaults.positionalThreshold,
): SwipeToDismissBoxState = rememberSaveable(
    saver = SwipeToDismissBoxState.Saver(positionalThreshold = positionalThreshold),
) {
    SwipeToDismissBoxState(initialValue, positionalThreshold)
}

/**
 * A composable that can be dismissed by swiping left or right.
 *
 * @param state The state of this component.
 * @param backgroundContent A composable that is stacked behind the [content] and is exposed when
 *   the content is swiped. You can/should use the [state] to have different backgrounds on each
 *   side.
 * @param modifier Optional [Modifier] for this component.
 * @param enableDismissFromStartToEnd Whether SwipeToDismissBox can be dismissed from start to end.
 * @param enableDismissFromEndToStart Whether SwipeToDismissBox can be dismissed from end to start.
 * @param gesturesEnabled Whether swipe-to-dismiss can be interacted by gestures.
 * @param onDismiss Optional callback to be called when [content] is dismissed. onDismissed provides
 *   the current dismissed direction.
 * @param content The content that can be dismissed.
 */
@Composable
fun SwipeToDismissBox(
    state: SwipeToDismissBoxState,
    backgroundContent: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    enableDismissFromStartToEnd: Boolean = true,
    enableDismissFromEndToStart: Boolean = true,
    gesturesEnabled: Boolean = true,
    onDismiss: suspend (SwipeToDismissBoxValue) -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier.anchoredHorizontalDraggable(
            state = state.anchoredDraggableState,
            enableDragFromStartToEnd = enableDismissFromStartToEnd,
            enableDragFromEndToStart = enableDismissFromEndToStart,
            enabled = gesturesEnabled && state.settledValue == SwipeToDismissBoxValue.Settled,
            flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                state = state.anchoredDraggableState,
                positionalThreshold = state.positionalThreshold,
            ),
        ),
        propagateMinConstraints = true,
    ) {
        Row(content = backgroundContent, modifier = Modifier.matchParentSize())
        Row(
            content = content,
            modifier = Modifier.draggableAnchors(state.anchoredDraggableState, Orientation.Horizontal) { size, _ ->
                DraggableAnchors {
                    val width = size.width.toFloat()
                    SwipeToDismissBoxValue.Settled at 0f
                    if (enableDismissFromStartToEnd) {
                        SwipeToDismissBoxValue.StartToEnd at width
                    }
                    if (enableDismissFromEndToStart) {
                        SwipeToDismissBoxValue.EndToStart at -width
                    }
                } to state.targetValue
            },
        )
    }
    LaunchedEffect(state.settledValue, onDismiss) {
        // Only call when state is settled in a dismissed direction.
        if (state.settledValue != SwipeToDismissBoxValue.Settled) {
            onDismiss(state.dismissDirection)
        }
    }
}
