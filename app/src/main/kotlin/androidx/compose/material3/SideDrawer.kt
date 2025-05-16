@file:Suppress("ktlint:standard:property-naming")

package androidx.compose.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.anchoredHorizontalDraggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.hippo.ehviewer.ui.tools.animateFloatMergeOneWayPredictiveBackAsState
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val AnimationSpec = TweenSpec<Float>(durationMillis = 256)

@Suppress("NotCloseable")
@Stable
class DrawerState2(initialValue: DrawerValue) {

    val anchoredDraggableState = AnchoredDraggableState(initialValue)

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
        fun Saver() = Saver<DrawerState2, DrawerValue>(
            save = { it.currentValue },
            restore = { DrawerState2(it) },
        )
    }
}

/**
 * Create and [remember] a [DrawerState2].
 *
 * @param initialValue The initial value of the state.
 */
@Composable
fun rememberDrawerState2(
    initialValue: DrawerValue,
): DrawerState2 = rememberSaveable(saver = DrawerState2.Saver()) {
    DrawerState2(initialValue)
}

@Composable
fun ModalSideDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState2 = rememberDrawerState2(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    scrimColor: Color = DrawerDefaults.scrimColor,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var maxValue by remember { mutableFloatStateOf(with(density) { DrawerDefaults.MaximumDrawerWidth.toPx() }) }
    val minValue = 0f

    SideEffect {
        drawerState.density = density
        drawerState.anchoredDraggableState.updateAnchors(
            DraggableAnchors {
                DrawerValue.Closed at maxValue
                DrawerValue.Open at minValue
            },
        )
    }

    Box(
        modifier.fillMaxSize().anchoredHorizontalDraggable(
            state = drawerState.anchoredDraggableState,
            enableDragFromStartToEnd = drawerState.isOpen,
            enableDragFromEndToStart = drawerState.isClosed,
            enabled = gesturesEnabled,
            flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                state = drawerState.anchoredDraggableState,
                animationSpec = AnimationSpec,
            ),
        ),
    ) {
        val radius by remember {
            snapshotFlow {
                val step = calculateFraction(maxValue, minValue, drawerState.currentOffset)
                lerp(0, 10, step).dp
            }
        }.collectAsState(0.dp)
        val blurModifier = Modifier.graphicsLayer {
            if (radius != 0.dp) {
                renderEffect = BlurEffect(radius.toPx(), radius.toPx(), TileMode.Clamp)
                shape = RectangleShape
                clip = true
            }
        }
        Box(modifier = blurModifier) {
            content()
        }
        Scrim(
            open = drawerState.isOpen,
            onClose = {
                if (gesturesEnabled) {
                    scope.launch { drawerState.close() }
                }
            },
            fraction = {
                calculateFraction(maxValue, minValue, drawerState.requireOffset())
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
                        reMeasured.placeRelative(placeable.width - scaledWidth, 0)
                    }
                }.background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(absoluteElevation),
                    shape = ShapeDefaults.Large.copy(topEnd = CornerSize(0), bottomEnd = CornerSize(0)),
                )
            } else {
                Modifier.graphicsLayer {
                    val scale = lerp(1f, 0.95f, predictiveState)
                    scaleX = scale
                    scaleY = scale
                }.offset {
                    IntOffset(lerp(0f, maxValue * 0.05f, predictiveState).roundToInt(), 0)
                }
            }
        } else {
            Modifier.onSizeChanged {
                val width = it.width
                if (width != 0) {
                    maxValue = width.toFloat()
                    drawerState.anchoredDraggableState.updateAnchors(
                        DraggableAnchors {
                            DrawerValue.Closed at maxValue
                            DrawerValue.Open at minValue
                        },
                    )
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
