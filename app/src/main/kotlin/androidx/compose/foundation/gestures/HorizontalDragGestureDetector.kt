/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.ViewConfiguration
import kotlin.coroutines.cancellation.CancellationException

/**
 * A Gesture detector that waits for pointer down and touch slop in the horizontal direction and
 * then calls [onDrag] for each drag event.
 * It follows the touch slop detection of [awaitHorizontalTouchSlopOrCancellation] but will consume
 * the position change automatically once the touch slop has been crossed, the amount of drag over
 * the touch slop is reported as the first drag event [onDrag] after the slop is crossed.
 * If [shouldAwaitTouchSlop] returns true the touch slop recognition phase will be ignored
 * and the drag gesture will be recognized immediately.The first [onDrag] in this case will report
 * an [Offset.Zero].
 *
 * [onDragStart] is called when the touch slop has been passed and includes an [Offset] representing
 * the last known pointer position relative to the containing element as well as  the initial
 * down event that triggered this gesture detection cycle. The [Offset] can be outside
 * the actual bounds of the element itself meaning the numbers can be negative or larger than the
 * element bounds if the touch target is smaller than the
 * [ViewConfiguration.minimumTouchTargetSize].
 *
 * [onDragEnd] is called after all pointers are up with the event change of the up event
 * and [onDragCancel] is called if another gesture has consumed pointer input,
 * canceling this gesture.
 *
 * @param onDragStart A lambda to be called when the drag gesture starts, it contains information
 * about the last known [PointerInputChange] relative to the containing element and the post slop
 * delta.
 * @param onDragEnd A lambda to be called when the gesture ends. It contains information about the
 * up [PointerInputChange] that finished the gesture.
 * @param onDragCancel A lambda to be called when the gesture is cancelled either by an error or
 * when it was consumed.
 * @param shouldAwaitTouchSlop Indicates if touch slop detection should be skipped.
 * @param onDrag A lambda to be called for each delta event in the gesture. It contains information
 * about the [PointerInputChange] and the movement offset.
 */
internal suspend fun PointerInputScope.detectHorizontalDragGestures(
    canDrag: (PointerInputChange) -> Boolean,
    onDragStart: (change: PointerInputChange, initialDelta: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    shouldAwaitTouchSlop: () -> Boolean,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        val initialDown =
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val awaitTouchSlop = shouldAwaitTouchSlop()

        if (!awaitTouchSlop) {
            initialDown.consume()
        }
        val down = awaitFirstDown(requireUnconsumed = false)
        var drag: PointerInputChange?
        var overSlop = Offset.Zero

        if (awaitTouchSlop) {
            do {
                drag = awaitHorizontalTouchSlopOrCancellation(
                    down.id,
                ) { change, over ->
                    if (canDrag.invoke(change)) {
                        change.consume()
                        overSlop = Offset(over, 0f)
                    } else {
                        throw CancellationException()
                    }
                }
            } while (drag != null && !drag.isConsumed)
        } else {
            drag = initialDown
        }

        if (drag != null) {
            onDragStart.invoke(drag, overSlop)
            onDrag(drag, overSlop)
            val dragEnd = horizontalDrag(
                pointerId = drag.id,
                onDrag = {
                    onDrag(it, it.positionChange())
                    it.consume()
                },
            )
            if (dragEnd) {
                onDragEnd()
            } else {
                onDragCancel()
            }
        }
    }
}
