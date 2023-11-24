@file:Suppress("ktlint:standard:property-naming", "ktlint:standard:function-naming")

package androidx.compose.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import com.hippo.ehviewer.ui.tools.animateFloatMergeOneWayPredictiveBackAsState
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val AnimationSpec = TweenSpec<Float>(durationMillis = 256)
private const val DrawerPositionalThreshold = 0.5f
private val DrawerVelocityThreshold = 400.dp
private val ContainerWidth = 360.0.dp

@Suppress("NotCloseable")
@Stable
class DrawerState2(
    initialValue: DrawerValue,
    val confirmStateChange: (DrawerValue) -> Boolean = { true },
) {

    val anchoredDraggableState = AnchoredDraggableState(
        initialValue = initialValue,
        animationSpec = AnimationSpec,
        confirmValueChange = confirmStateChange,
        positionalThreshold = { distance -> distance * DrawerPositionalThreshold },
        velocityThreshold = { with(requireDensity()) { DrawerVelocityThreshold.toPx() } },
    )

    /**
     * Whether the drawer is open.
     */
    val isOpen: Boolean
        get() = currentValue == DrawerValue.Open

    /**
     * Whether the drawer is closed.
     */
    val isClosed: Boolean
        get() = currentValue == DrawerValue.Closed

    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the start the drawer
     * currently in. If a swipe or an animation is in progress, this corresponds the state drawer
     * was in before the swipe or animation started.
     */
    val currentValue: DrawerValue
        get() {
            return anchoredDraggableState.currentValue
        }

    /**
     * Whether the state is currently animating.
     */
    val isAnimationRunning: Boolean
        get() {
            return anchoredDraggableState.isAnimationRunning
        }

    /**
     * Open the drawer with animation and suspend until it if fully opened or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the open animation ended
     */
    suspend fun open() = animateTo(DrawerValue.Open)

    /**
     * Close the drawer with animation and suspend until it if fully closed or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the close animation ended
     */
    suspend fun close() = animateTo(DrawerValue.Closed)

    /**
     * Set the state of the drawer with specific animation
     *
     * @param targetValue The new value to animate to.
     * @param anim The animation that will be used to animate to the new value.
     */
    @Deprecated(
        message = "This method has been replaced by the open and close methods. The animation " +
            "spec is now an implementation detail of ModalDrawer.",
    )
    suspend fun animateTo(targetValue: DrawerValue, anim: AnimationSpec<Float>) {
        animateTo(targetValue = targetValue, animationSpec = anim)
    }

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value
     */
    suspend fun snapTo(targetValue: DrawerValue) {
        anchoredDraggableState.snapTo(targetValue)
    }

    /**
     * The target value of the drawer state.
     *
     * If a swipe is in progress, this is the value that the Drawer would animate to if the
     * swipe finishes. If an animation is running, this is the target value of that animation.
     * Finally, if no swipe or animation is in progress, this is the same as the [currentValue].
     */
    val targetValue: DrawerValue
        get() = anchoredDraggableState.targetValue

    /**
     * The current position (in pixels) of the drawer sheet, or Float.NaN before the offset is
     * initialized.
     *
     * @see [AnchoredDraggableState.offset] for more information.
     */
    @Deprecated(
        message = "Please access the offset through currentOffset, which returns the value " +
            "directly instead of wrapping it in a state object.",
        replaceWith = ReplaceWith("currentOffset"),
    )
    val offset: State<Float> = object : State<Float> {
        override val value: Float get() = anchoredDraggableState.offset
    }

    /**
     * The current position (in pixels) of the drawer sheet, or Float.NaN before the offset is
     * initialized.
     *
     * @see [AnchoredDraggableState.offset] for more information.
     */
    val currentOffset: Float get() = anchoredDraggableState.offset

    internal var density: Density? by mutableStateOf(null)

    private fun requireDensity() = requireNotNull(density) {
        "The density on BottomDrawerState ($this) was not set. Did you use BottomDrawer" +
            " with the BottomDrawer composable?"
    }

    internal fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    private suspend fun animateTo(
        targetValue: DrawerValue,
        animationSpec: AnimationSpec<Float> = AnimationSpec,
        velocity: Float = anchoredDraggableState.lastVelocity,
    ) {
        anchoredDraggableState.anchoredDrag(targetValue = targetValue) { anchors, latestTarget ->
            val targetOffset = anchors.positionOf(latestTarget)
            if (!targetOffset.isNaN()) {
                var prev = if (currentOffset.isNaN()) 0f else currentOffset
                animate(prev, targetOffset, velocity, animationSpec) { value, velocity ->
                    // Our onDrag coerces the value within the bounds, but an animation may
                    // overshoot, for example a spring animation or an overshooting interpolator
                    // We respect the user's intention and allow the overshoot, but still use
                    // DraggableState's drag for its mutex.
                    dragTo(value, velocity)
                    prev = value
                }
            }
        }
    }

    companion object {
        /**
         * The default [Saver] implementation for [DrawerState2].
         */
        fun Saver(confirmStateChange: (DrawerValue) -> Boolean) =
            Saver<DrawerState2, DrawerValue>(
                save = { it.currentValue },
                restore = { DrawerState2(it, confirmStateChange) },
            )
    }
}

/**
 * Create and [remember] a [DrawerState2].
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
fun rememberDrawerState(
    initialValue: DrawerValue,
    confirmStateChange: (DrawerValue) -> Boolean = { true },
): DrawerState2 {
    return rememberSaveable(saver = DrawerState2.Saver(confirmStateChange)) {
        DrawerState2(initialValue, confirmStateChange)
    }
}

@Composable
fun ModalNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState2 = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    scrimColor: Color = DrawerDefaults.scrimColor,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var minValue by remember { mutableStateOf(-with(density) { ContainerWidth.toPx() }) }
    val maxValue = 0f

    SideEffect {
        drawerState.density = density
        drawerState.anchoredDraggableState.updateAnchors(
            DraggableAnchors {
                DrawerValue.Closed at minValue
                DrawerValue.Open at maxValue
            },
        )
    }

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val velocityTracker = remember { VelocityTracker() }
    val dragModifier = if (gesturesEnabled) {
        Modifier.pointerInput(isRtl) {
            val multiplier = if (isRtl) -1 else 1
            awaitEachGesture {
                fun canConsume(slop: Float) = (slop > 0 && drawerState.isClosed) || (slop < 0 && drawerState.isOpen)
                val down = awaitFirstDown(requireUnconsumed = false)
                var ignore = false
                val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                    val overSlop = over * multiplier
                    val canConsume = canConsume(overSlop)
                    if (canConsume && !ignore) {
                        velocityTracker.addPointerInputChange(change)
                        change.consume()
                        drawerState.anchoredDraggableState.dispatchRawDelta(overSlop)
                    } else {
                        ignore = true
                    }
                }
                if (drag != null) {
                    horizontalDrag(drag.id) {
                        velocityTracker.addPointerInputChange(it)
                        drawerState.anchoredDraggableState.dispatchRawDelta(it.positionChange().x * multiplier)
                    }
                    val velocity = velocityTracker.calculateVelocity()
                    velocityTracker.resetTracking()
                    scope.launch {
                        drawerState.anchoredDraggableState.settle(velocity.x * multiplier)
                    }
                }
            }
        }
    } else {
        Modifier
    }
    Box(modifier.fillMaxSize().then(dragModifier)) {
        val step = calculateFraction(minValue, maxValue, drawerState.currentOffset)
        val radius = lerp(0.dp, 10.dp, step)
        Box(modifier = Modifier.blur(radius)) {
            content()
        }
        Scrim(
            open = drawerState.isOpen,
            onClose = {
                if (gesturesEnabled && drawerState.confirmStateChange(DrawerValue.Closed)) {
                    scope.launch { drawerState.close() }
                }
            },
            fraction = {
                calculateFraction(minValue, maxValue, drawerState.requireOffset())
            },
            color = scrimColor,
        )
        val predictiveState by animateFloatMergeOneWayPredictiveBackAsState(drawerState.isOpen) {
            drawerState.close()
        }
        val absoluteElevation = LocalAbsoluteTonalElevation.current + DrawerDefaults.ModalDrawerElevation
        val predictiveModifier = if (drawerState.isOpen) {
            if (predictiveState > 0) {
                Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val multiplierX = lerp(1f, 1.05f, predictiveState)
                    val multiplierY = lerp(1f, 0.95f, predictiveState)
                    val scaledWidth = (multiplierX * placeable.width).roundToInt()
                    val scaledHeight = (multiplierY * placeable.height).roundToInt()
                    val reMeasured = measurable.measure(Constraints.fixed(scaledWidth, scaledHeight))
                    layout(scaledWidth, scaledHeight) {
                        reMeasured.placeRelative(scaledWidth - placeable.width, 0)
                    }
                }.background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(absoluteElevation),
                    shape = DrawerDefaults.shape,
                )
            } else {
                Modifier.scale(lerp(1f, 0.95f, -predictiveState)).offset {
                    IntOffset(lerp(0f, minValue * 0.05f, -predictiveState).roundToInt(), 0)
                }
            }
        } else {
            Modifier.onGloballyPositioned {
                val w = it.size.width
                if (w != 0) {
                    minValue = -it.size.width.toFloat()
                }
            }
        }
        Box(
            modifier = Modifier.offset {
                IntOffset(drawerState.requireOffset().roundToInt(), 0)
            } then predictiveModifier.align(Alignment.CenterStart),
            contentAlignment = Alignment.CenterEnd,
        ) {
            drawerContent()
        }
    }
}

@Composable
fun SideDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState2 = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    scrimColor: Color = DrawerDefaults.scrimColor,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var minValue by remember { mutableStateOf(with(density) { ContainerWidth.toPx() }) }
    val maxValue = 0f

    SideEffect {
        drawerState.density = density
        drawerState.anchoredDraggableState.updateAnchors(
            DraggableAnchors {
                DrawerValue.Closed at minValue
                DrawerValue.Open at maxValue
            },
        )
    }

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val velocityTracker = remember { VelocityTracker() }
    val dragModifier = if (gesturesEnabled) {
        Modifier.pointerInput(isRtl) {
            val multiplier = if (isRtl) -1 else 1
            awaitEachGesture {
                fun canConsume(slop: Float) = (slop > 0 && drawerState.isOpen) || (slop < 0 && drawerState.isClosed)
                val down = awaitFirstDown(requireUnconsumed = false)
                var ignore = false
                val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                    val overSlop = over * multiplier
                    val canConsume = canConsume(overSlop)
                    if (canConsume && !ignore) {
                        velocityTracker.addPointerInputChange(change)
                        change.consume()
                        drawerState.anchoredDraggableState.dispatchRawDelta(overSlop)
                    } else {
                        ignore = true
                    }
                }
                if (drag != null) {
                    horizontalDrag(drag.id) {
                        velocityTracker.addPointerInputChange(it)
                        drag.consume()
                        drawerState.anchoredDraggableState.dispatchRawDelta(it.positionChange().x * multiplier)
                    }
                    val velocity = velocityTracker.calculateVelocity()
                    velocityTracker.resetTracking()
                    scope.launch {
                        drawerState.anchoredDraggableState.settle(velocity.x * multiplier)
                    }
                }
            }
        }
    } else {
        Modifier
    }
    Box(modifier.fillMaxSize().then(dragModifier)) {
        val step = calculateFraction(minValue, maxValue, drawerState.currentOffset)
        val radius = lerp(0.dp, 10.dp, step)
        Box(modifier = Modifier.blur(radius)) {
            content()
        }
        Scrim(
            open = drawerState.isOpen,
            onClose = {
                if (gesturesEnabled && drawerState.confirmStateChange(DrawerValue.Closed)) {
                    scope.launch { drawerState.close() }
                }
            },
            fraction = {
                calculateFraction(minValue, maxValue, drawerState.requireOffset())
            },
            color = scrimColor,
        )
        val predictiveState by animateFloatMergeOneWayPredictiveBackAsState(drawerState.isOpen) {
            drawerState.close()
        }
        val absoluteElevation = LocalAbsoluteTonalElevation.current + DrawerDefaults.ModalDrawerElevation
        val predictiveModifier = if (drawerState.isOpen) {
            if (predictiveState < 0) {
                Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val multiplierX = lerp(1f, 1.05f, -predictiveState)
                    val multiplierY = lerp(1f, 0.95f, -predictiveState)
                    val scaledWidth = (multiplierX * placeable.width).roundToInt()
                    val scaledHeight = (multiplierY * placeable.height).roundToInt()
                    val reMeasured = measurable.measure(Constraints.fixed(scaledWidth, scaledHeight))
                    layout(scaledWidth, scaledHeight) {
                        reMeasured.placeRelative(-(scaledWidth - placeable.width), 0)
                    }
                }.background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(absoluteElevation),
                    shape = ShapeDefaults.Large.copy(topEnd = CornerSize(0), bottomEnd = CornerSize(0)),
                )
            } else {
                Modifier.scale(lerp(1f, 0.95f, predictiveState)).offset {
                    IntOffset(lerp(0f, minValue * 0.05f, predictiveState).roundToInt(), 0)
                }
            }
        } else {
            Modifier.onGloballyPositioned {
                val w = it.size.width
                if (w != 0) {
                    minValue = it.size.width.toFloat()
                }
            }
        }
        Box(
            modifier = Modifier.offset {
                IntOffset(drawerState.requireOffset().roundToInt(), 0)
            }.align(Alignment.CenterEnd) then predictiveModifier,
            contentAlignment = Alignment.CenterStart,
        ) {
            drawerContent()
        }
    }
}

@Suppress("SameParameterValue")
private fun calculateFraction(a: Float, b: Float, pos: Float) = ((pos - a) / (b - a)).coerceIn(0f, 1f)

@Composable
private fun Scrim(open: Boolean, onClose: () -> Unit, fraction: () -> Float, color: Color) {
    val dismissDrawer = if (open) {
        Modifier.pointerInput(onClose) { detectTapGestures { onClose() } }
    } else {
        Modifier
    }

    Canvas(Modifier.fillMaxSize().then(dismissDrawer)) {
        drawRect(color, alpha = fraction())
    }
}
