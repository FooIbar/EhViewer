@file:Suppress("ktlint:standard:property-naming", "ktlint:standard:function-naming")

package androidx.compose.material3

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.popup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hippo.ehviewer.ui.tools.animateFloatMergeOneWayPredictiveBackAsState
import com.hippo.ehviewer.util.isAtLeastT
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Stable
@ExperimentalMaterial3Api
internal object AnchoredDraggableDefaults2 {
    /**
     * The default animation used by [AnchoredDraggableState].
     */
    @get:ExperimentalMaterial3Api
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalMaterial3Api
    val AnimationSpec = SpringSpec<Float>()
}

/**
 * State of a sheet composable, such as [ModalBottomSheetFix]
 *
 * Contains states relating to its swipe position as well as animations between state values.
 *
 * @param skipPartiallyExpanded Whether the partially expanded state, if the sheet is large
 * enough, should be skipped. If true, the sheet will always expand to the [Expanded] state and move
 * to the [Hidden] state if available when hiding the sheet, either programmatically or by user
 * interaction.
 * @param initialValue The initial value of the state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 * @param skipHiddenState Whether the hidden state should be skipped. If true, the sheet will always
 * expand to the [Expanded] state and move to the [PartiallyExpanded] if available, either
 * programmatically or by user interaction.
 */
@Stable
@ExperimentalMaterial3Api
class SheetState2
@Deprecated(
    message = "This constructor is deprecated. " +
        "Please use the constructor that provides a [Density]",
    replaceWith = ReplaceWith(
        "SheetState(" +
            "skipPartiallyExpanded, LocalDensity.current, initialValue, " +
            "confirmValueChange, skipHiddenState)",
    ),
)
constructor(
    internal val skipPartiallyExpanded: Boolean,
    initialValue: SheetValue = SheetValue.Hidden,
    val confirmValueChange: (SheetValue) -> Boolean = { true },
    internal val skipHiddenState: Boolean = false,
) {

    /**
     * State of a sheet composable, such as [ModalBottomSheetFix]
     *
     * Contains states relating to its swipe position as well as animations between state values.
     *
     * @param skipPartiallyExpanded Whether the partially expanded state, if the sheet is large
     * enough, should be skipped. If true, the sheet will always expand to the [Expanded] state and move
     * to the [Hidden] state if available when hiding the sheet, either programmatically or by user
     * interaction.
     * @param initialValue The initial value of the state.
     * @param density The density that this state can use to convert values to and from dp.
     * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
     * @param skipHiddenState Whether the hidden state should be skipped. If true, the sheet will always
     * expand to the [Expanded] state and move to the [PartiallyExpanded] if available, either
     * programmatically or by user interaction.
     */
    @ExperimentalMaterial3Api
    @Suppress("Deprecation")
    constructor(
        skipPartiallyExpanded: Boolean,
        density: Density,
        initialValue: SheetValue = SheetValue.Hidden,
        confirmValueChange: (SheetValue) -> Boolean = { true },
        skipHiddenState: Boolean = false,
    ) : this(skipPartiallyExpanded, initialValue, confirmValueChange, skipHiddenState) {
        this.density = density
    }
    init {
        if (skipPartiallyExpanded) {
            require(initialValue != SheetValue.PartiallyExpanded) {
                "The initial value must not be set to PartiallyExpanded if skipPartiallyExpanded " +
                    "is set to true."
            }
        }
        if (skipHiddenState) {
            require(initialValue != SheetValue.Hidden) {
                "The initial value must not be set to Hidden if skipHiddenState is set to true."
            }
        }
    }

    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the state the bottom sheet is
     * currently in. If a swipe or an animation is in progress, this corresponds the state the sheet
     * was in before the swipe or animation started.
     */

    val currentValue: SheetValue get() = anchoredDraggableState.currentValue

    /**
     * The target value of the bottom sheet state.
     *
     * If a swipe is in progress, this is the value that the sheet would animate to if the
     * swipe finishes. If an animation is running, this is the target value of that animation.
     * Finally, if no swipe or animation is in progress, this is the same as the [currentValue].
     */
    val targetValue: SheetValue get() = anchoredDraggableState.targetValue

    /**
     * Whether the modal bottom sheet is visible.
     */
    val isVisible: Boolean
        get() = anchoredDraggableState.currentValue != SheetValue.Hidden

    /**
     * Require the current offset (in pixels) of the bottom sheet.
     *
     * The offset will be initialized during the first measurement phase of the provided sheet
     * content.
     *
     * These are the phases:
     * Composition { -> Effects } -> Layout { Measurement -> Placement } -> Drawing
     *
     * During the first composition, an [IllegalStateException] is thrown. In subsequent
     * compositions, the offset will be derived from the anchors of the previous pass. Always prefer
     * accessing the offset from a LaunchedEffect as it will be scheduled to be executed the next
     * frame, after layout.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    /**
     * Whether the sheet has an expanded state defined.
     */

    val hasExpandedState: Boolean
        get() = anchoredDraggableState.anchors.hasAnchorFor(SheetValue.Expanded)

    /**
     * Whether the modal bottom sheet has a partially expanded state defined.
     */
    val hasPartiallyExpandedState: Boolean
        get() = anchoredDraggableState.anchors.hasAnchorFor(SheetValue.PartiallyExpanded)

    /**
     * Fully expand the bottom sheet with animation and suspend until it is fully expanded or
     * animation has been cancelled.
     * *
     * @throws [CancellationException] if the animation is interrupted
     */
    suspend fun expand() {
        anchoredDraggableState.animateTo(SheetValue.Expanded)
    }

    /**
     * Animate the bottom sheet and suspend until it is partially expanded or animation has been
     * cancelled.
     * @throws [CancellationException] if the animation is interrupted
     * @throws [IllegalStateException] if [skipPartiallyExpanded] is set to true
     */
    suspend fun partialExpand() {
        check(!skipPartiallyExpanded) {
            "Attempted to animate to partial expanded when skipPartiallyExpanded was enabled. Set" +
                " skipPartiallyExpanded to false to use this function."
        }
        animateTo(SheetValue.PartiallyExpanded)
    }

    /**
     * Expand the bottom sheet with animation and suspend until it is [PartiallyExpanded] if defined
     * else [Expanded].
     * @throws [CancellationException] if the animation is interrupted
     */
    suspend fun show() {
        val targetValue = when {
            hasPartiallyExpandedState -> SheetValue.PartiallyExpanded
            else -> SheetValue.Expanded
        }
        animateTo(targetValue)
    }

    /**
     * Hide the bottom sheet with animation and suspend until it is fully hidden or animation has
     * been cancelled.
     * @throws [CancellationException] if the animation is interrupted
     */
    suspend fun hide() {
        check(!skipHiddenState) {
            "Attempted to animate to hidden when skipHiddenState was enabled. Set skipHiddenState" +
                " to false to use this function."
        }
        animateTo(SheetValue.Hidden)
    }

    /**
     * Animate to a [targetValue].
     * If the [targetValue] is not in the set of anchors, the [currentValue] will be updated to the
     * [targetValue] without updating the offset.
     *
     * @throws CancellationException if the interaction interrupted by another interaction like a
     * gesture interaction or another programmatic interaction like a [animateTo] or [snapTo] call.
     *
     * @param targetValue The target value of the animation
     */
    internal suspend fun animateTo(
        targetValue: SheetValue,
        velocity: Float = anchoredDraggableState.lastVelocity,
    ) {
        anchoredDraggableState.animateTo(targetValue, velocity)
    }

    /**
     * Snap to a [targetValue] without any animation.
     *
     * @throws CancellationException if the interaction interrupted by another interaction like a
     * gesture interaction or another programmatic interaction like a [animateTo] or [snapTo] call.
     *
     * @param targetValue The target value of the animation
     */
    internal suspend fun snapTo(targetValue: SheetValue) {
        anchoredDraggableState.snapTo(targetValue)
    }

    /**
     * Find the closest anchor taking into account the velocity and settle at it with an animation.
     */
    internal suspend fun settle(velocity: Float) {
        anchoredDraggableState.settle(velocity)
    }

    internal var anchoredDraggableState = AnchoredDraggableState(
        initialValue = initialValue,
        animationSpec = AnchoredDraggableDefaults2.AnimationSpec,
        confirmValueChange = confirmValueChange,
        positionalThreshold = { with(requireDensity()) { 56.dp.toPx() } },
        velocityThreshold = { with(requireDensity()) { 125.dp.toPx() } },
    )

    internal val offset: Float get() = anchoredDraggableState.offset

    internal var density: Density? = null
    private fun requireDensity() = requireNotNull(density) {
        "SheetState did not have a density attached. Are you using SheetState with " +
            "BottomSheetScaffold or ModalBottomSheet component?"
    }

    companion object {
        /**
         * The default [Saver] implementation for [SheetState2].
         */
        fun Saver(
            skipPartiallyExpanded: Boolean,
            confirmValueChange: (SheetValue) -> Boolean,
            density: Density,
        ) = Saver<SheetState2, SheetValue>(
            save = { it.currentValue },
            restore = { savedValue ->
                SheetState2(skipPartiallyExpanded, density, savedValue, confirmValueChange)
            },
        )

        /**
         * The default [Saver] implementation for [SheetState2].
         */
        @Deprecated(
            message = "This function is deprecated. Please use the overload where Density is" +
                " provided.",
            replaceWith = ReplaceWith(
                "Saver(skipPartiallyExpanded, confirmValueChange, LocalDensity.current)",
            ),
        )
        @Suppress("Deprecation")
        fun Saver(
            skipPartiallyExpanded: Boolean,
            confirmValueChange: (SheetValue) -> Boolean,
        ) = Saver<SheetState2, SheetValue>(
            save = { it.currentValue },
            restore = { savedValue ->
                SheetState2(skipPartiallyExpanded, savedValue, confirmValueChange)
            },
        )
    }
}

/**
 * <a href="https://m3.material.io/components/bottom-sheets/overview" class="external" target="_blank">Material Design modal bottom sheet</a>.
 *
 * Modal bottom sheets are used as an alternative to inline menus or simple dialogs on mobile,
 * especially when offering a long list of action items, or when items require longer descriptions
 * and icons. Like dialogs, modal bottom sheets appear in front of app content, disabling all other
 * app functionality when they appear, and remaining on screen until confirmed, dismissed, or a
 * required action has been taken.
 *
 * ![Bottom sheet image](https://developer.android.com/images/reference/androidx/compose/material3/bottom_sheet.png)
 *
 * A simple example of a modal bottom sheet looks like this:
 *
 * @sample androidx.compose.material3.samples.ModalBottomSheetSample
 *
 * @param onDismissRequest Executes when the user clicks outside of the bottom sheet, after sheet
 * animates to [Hidden].
 * @param modifier Optional [Modifier] for the bottom sheet.
 * @param sheetState The state of the bottom sheet.
 * @param sheetMaxWidth [Dp] that defines what the maximum width the sheet will take.
 * Pass in [Dp.Unspecified] for a sheet that spans the entire screen width.
 * @param shape The shape of the bottom sheet.
 * @param containerColor The color used for the background of this bottom sheet
 * @param contentColor The preferred color for content inside this bottom sheet. Defaults to either
 * the matching content color for [containerColor], or to the current [LocalContentColor] if
 * [containerColor] is not a color from the theme.
 * @param tonalElevation The tonal elevation of this bottom sheet.
 * @param scrimColor Color of the scrim that obscures content when the bottom sheet is open.
 * @param dragHandle Optional visual marker to swipe the bottom sheet.
 * @param windowInsets window insets to be passed to the bottom sheet window via [PaddingValues]
 * params.
 * @param properties [ModalBottomSheetProperties2] for further customization of this
 * modal bottom sheet's behavior.
 * @param content The content to be displayed inside the bottom sheet.
 */
@Composable
@ExperimentalMaterial3Api
fun ModalBottomSheetFix(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState2 = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    windowInsets: WindowInsets = BottomSheetDefaults.windowInsets,
    properties: ModalBottomSheetProperties2 = ModalBottomSheetDefaults2.properties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    // b/291735717 Remove this once deprecated methods without density are removed
    val density = LocalDensity.current
    SideEffect {
        sheetState.density = density
    }
    val scope = rememberCoroutineScope()
    val animateToDismiss: () -> Unit = {
        if (sheetState.confirmValueChange(SheetValue.Hidden)) {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    onDismissRequest()
                }
            }
        }
    }
    LaunchedEffect(sheetState) {
        snapshotFlow {
            sheetState.targetValue == SheetValue.Hidden && !sheetState.isVisible
        }.drop(1).collectLatest {
            if (it) onDismissRequest()
        }
    }
    val settleToDismiss: (velocity: Float) -> Unit = {
        scope.launch { sheetState.settle(it) }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismissRequest()
        }
    }

    val popupOnDismissRequest: () -> Unit = {
        if (sheetState.currentValue == SheetValue.Expanded && sheetState.hasPartiallyExpandedState) {
            scope.launch { sheetState.partialExpand() }
        } else { // Is expanded without collapsed state or is collapsed.
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
        }
    }

    ModalBottomSheetPopup(
        properties = properties,
        onDismissRequest = popupOnDismissRequest,
        windowInsets = windowInsets,
    ) {
        val configuration = LocalConfiguration.current
        val widthDp = configuration.screenWidthDp.dp
        val height = configuration.screenHeightDp.dp
        val predictiveState by animateFloatMergeOneWayPredictiveBackAsState(true) {
            sheetState.hide()
            onDismissRequest()
        }
        val predictiveModifier = Modifier.graphicsLayer {
            val scale = lerp(1f, 0.90f, predictiveState.absoluteValue)
            scaleX = scale
            scaleY = scale
        }.offset {
            val ofsY = lerp(0f, height.toPx() * 0.0625f, predictiveState.absoluteValue).roundToInt()
            val ofsX = lerp(0f, widthDp.toPx() * 0.025f, predictiveState).roundToInt()
            IntOffset(ofsX, ofsY)
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val fullHeight = constraints.maxHeight
            Scrim(
                color = scrimColor,
                onDismissRequest = animateToDismiss,
                visible = sheetState.targetValue != SheetValue.Hidden,
            )
            Surface(
                modifier = modifier
                    .widthIn(max = sheetMaxWidth)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(
                            0,
                            sheetState
                                .requireOffset()
                                .toInt(),
                        )
                    } then predictiveModifier
                    .nestedScroll(
                        remember(sheetState) {
                            ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                                sheetState = sheetState,
                                orientation = Orientation.Vertical,
                                onFling = settleToDismiss,
                            )
                        },
                    )
                    .anchoredDraggable(
                        state = sheetState.anchoredDraggableState,
                        orientation = Orientation.Vertical,
                        enabled = sheetState.isVisible,
                        startDragImmediately = sheetState.anchoredDraggableState.isAnimationRunning,
                    )
                    .modalBottomSheetAnchors(
                        sheetState = sheetState,
                        fullHeight = fullHeight.toFloat(),
                    ),
                shape = shape,
                color = containerColor,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    if (dragHandle != null) {
                        Box(Modifier.align(Alignment.CenterHorizontally)) {
                            dragHandle()
                        }
                    }
                    content()
                }
            }
        }
    }
    if (sheetState.hasExpandedState) {
        LaunchedEffect(sheetState) {
            sheetState.show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
    sheetState: SheetState2,
    orientation: Orientation,
    onFling: (velocity: Float) -> Unit,
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.toFloat()
        return if (delta < 0 && source == NestedScrollSource.Drag) {
            sheetState.anchoredDraggableState.dispatchRawDelta(delta).toOffset()
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        return if (source == NestedScrollSource.Drag) {
            sheetState.anchoredDraggableState.dispatchRawDelta(available.toFloat()).toOffset()
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val toFling = available.toFloat()
        val currentOffset = sheetState.requireOffset()
        val minAnchor = sheetState.anchoredDraggableState.anchors.minAnchor()
        return if (toFling < 0 && currentOffset > minAnchor) {
            onFling(toFling)
            // since we go to the anchor with tween settling, consume all for the best UX
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        onFling(available.toFloat())
        return available
    }

    private fun Float.toOffset(): Offset = Offset(
        x = if (orientation == Orientation.Horizontal) this else 0f,
        y = if (orientation == Orientation.Vertical) this else 0f,
    )

    @JvmName("velocityToFloat")
    private fun Velocity.toFloat() = if (orientation == Orientation.Horizontal) x else y

    @JvmName("offsetToFloat")
    private fun Offset.toFloat(): Float = if (orientation == Orientation.Horizontal) x else y
}

/**
 * Properties used to customize the behavior of a [ModalBottomSheetFix].
 *
 * @param securePolicy Policy for setting [WindowManager.LayoutParams.FLAG_SECURE] on the bottom
 * sheet's window.
 * @param isFocusable Whether the modal bottom sheet is focusable. When true,
 * the modal bottom sheet will receive IME events and key presses, such as when
 * the back button is pressed.
 * @param shouldDismissOnBackPress Whether the modal bottom sheet can be dismissed by pressing
 * the back button. If true, pressing the back button will call onDismissRequest.
 * Note that [isFocusable] must be set to true in order to receive key events such as
 * the back button - if the modal bottom sheet is not focusable then this property does nothing.
 */
@ExperimentalMaterial3Api
class ModalBottomSheetProperties2(
    val securePolicy: SecureFlagPolicy,
    val isFocusable: Boolean,
    val shouldDismissOnBackPress: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModalBottomSheetProperties2) return false

        if (securePolicy != other.securePolicy) return false
        if (isFocusable != other.isFocusable) return false
        if (shouldDismissOnBackPress != other.shouldDismissOnBackPress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = securePolicy.hashCode()
        result = 31 * result + isFocusable.hashCode()
        result = 31 * result + shouldDismissOnBackPress.hashCode()
        return result
    }
}

/**
 * Default values for [ModalBottomSheetFix]
 */
@Immutable
@ExperimentalMaterial3Api
object ModalBottomSheetDefaults2 {
    /**
     * Properties used to customize the behavior of a [ModalBottomSheetFix].
     *
     * @param securePolicy Policy for setting [WindowManager.LayoutParams.FLAG_SECURE] on the bottom
     * sheet's window.
     * @param isFocusable Whether the modal bottom sheet is focusable. When true,
     * the modal bottom sheet will receive IME events and key presses, such as when
     * the back button is pressed.
     * @param shouldDismissOnBackPress Whether the modal bottom sheet can be dismissed by pressing
     * the back button. If true, pressing the back button will call onDismissRequest.
     * Note that [isFocusable] must be set to true in order to receive key events such as
     * the back button - if the modal bottom sheet is not focusable then this property does nothing.
     */
    fun properties(
        securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
        isFocusable: Boolean = true,
        shouldDismissOnBackPress: Boolean = true,
    ) = ModalBottomSheetProperties2(securePolicy, isFocusable, shouldDismissOnBackPress)
}

/**
 * Create and [remember] a [SheetState2] for [ModalBottomSheetFix].
 *
 * @param skipPartiallyExpanded Whether the partially expanded state, if the sheet is tall enough,
 * should be skipped. If true, the sheet will always expand to the [Expanded] state and move to the
 * [Hidden] state when hiding the sheet, either programmatically or by user interaction.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
@ExperimentalMaterial3Api
fun rememberModalBottomSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
) = rememberSheetState(skipPartiallyExpanded, confirmValueChange, SheetValue.Hidden)

@Composable
@ExperimentalMaterial3Api
internal fun rememberSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
    initialValue: SheetValue = SheetValue.Hidden,
    skipHiddenState: Boolean = false,
): SheetState2 {
    val density = LocalDensity.current
    return rememberSaveable(
        skipPartiallyExpanded,
        confirmValueChange,
        saver = SheetState2.Saver(
            skipPartiallyExpanded = skipPartiallyExpanded,
            confirmValueChange = confirmValueChange,
            density = density,
        ),
    ) {
        SheetState2(
            skipPartiallyExpanded,
            density,
            initialValue,
            confirmValueChange,
            skipHiddenState,
        )
    }
}

@Composable
private fun Scrim(
    color: Color,
    onDismissRequest: () -> Unit,
    visible: Boolean,
) {
    if (color.isSpecified) {
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = TweenSpec(),
            label = "scrim",
        )
        val dismissSheet = if (visible) {
            Modifier
                .pointerInput(onDismissRequest) {
                    detectTapGestures {
                        onDismissRequest()
                    }
                }
                .clearAndSetSemantics {}
        } else {
            Modifier
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .then(dismissSheet),
        ) {
            drawRect(color = color, alpha = alpha)
        }
    }
}

@ExperimentalMaterial3Api
private fun Modifier.modalBottomSheetAnchors(
    sheetState: SheetState2,
    fullHeight: Float,
) = onSizeChanged { sheetSize ->

    val newAnchors = DraggableAnchors {
        SheetValue.Hidden at fullHeight
        if (sheetSize.height > (fullHeight / 2) && !sheetState.skipPartiallyExpanded) {
            SheetValue.PartiallyExpanded at fullHeight / 2f
        }
        if (sheetSize.height != 0) {
            SheetValue.Expanded at max(0f, fullHeight - sheetSize.height)
        }
    }

    val newTarget = when (sheetState.anchoredDraggableState.targetValue) {
        SheetValue.Hidden -> SheetValue.Hidden
        SheetValue.PartiallyExpanded, SheetValue.Expanded -> {
            val hasPartiallyExpandedState = newAnchors.hasAnchorFor(SheetValue.PartiallyExpanded)
            val newTarget = if (hasPartiallyExpandedState) {
                SheetValue.PartiallyExpanded
            } else if (newAnchors.hasAnchorFor(SheetValue.Expanded)) SheetValue.Expanded else SheetValue.Hidden
            newTarget
        }
    }

    sheetState.anchoredDraggableState.updateAnchors(newAnchors, newTarget)
}

/**
 * Popup specific for modal bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModalBottomSheetPopup(
    properties: ModalBottomSheetProperties2,
    onDismissRequest: () -> Unit,
    windowInsets: WindowInsets,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val id = rememberSaveable { UUID.randomUUID() }
    val parentComposition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val layoutDirection = LocalLayoutDirection.current
    val configuration = LocalConfiguration.current
    val modalBottomSheetWindow = remember(configuration) {
        ModalBottomSheetWindow2(
            properties = properties,
            onDismissRequest = onDismissRequest,
            composeView = view,
            saveId = id,
        ).apply {
            setCustomContent(
                parent = parentComposition,
                content = {
                    Box(
                        Modifier
                            .semantics { this.popup() }
                            .windowInsetsPadding(windowInsets)
                            .then(
                                // TODO(b/290893168): Figure out a solution for APIs < 30.
                                if (Build.VERSION.SDK_INT >= 33) {
                                    Modifier.imePadding()
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        currentContent()
                    }
                },
            )
        }
    }

    DisposableEffect(modalBottomSheetWindow) {
        modalBottomSheetWindow.show()
        modalBottomSheetWindow.superSetLayoutDirection(layoutDirection)
        onDispose {
            modalBottomSheetWindow.disposeComposition()
            modalBottomSheetWindow.dismiss()
        }
    }
}

/** Custom compose view for [ModalBottomSheetFix] */
@SuppressLint("ViewConstructor")
@OptIn(ExperimentalMaterial3Api::class)
private class ModalBottomSheetWindow2(
    private val properties: ModalBottomSheetProperties2,
    private var onDismissRequest: () -> Unit,
    private val composeView: View,
    saveId: UUID,
) : AbstractComposeView(composeView.context),
    ViewTreeObserver.OnGlobalLayoutListener,
    ViewRootForInspector,
    OnBackPressedDispatcherOwner {

    init {
        id = android.R.id.content
        // Set up view owners
        setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        setViewTreeSavedStateRegistryOwner(composeView.findViewTreeSavedStateRegistryOwner())
        setTag(androidx.compose.ui.R.id.compose_view_saveable_id_tag, "Popup:$saveId")
        // Enable children to draw their shadow by not clipping them
        clipChildren = false
    }

    override val lifecycle = composeView.findViewTreeLifecycleOwner()!!.lifecycle
    override val onBackPressedDispatcher = OnBackPressedDispatcher {
        onDismissRequest()
    }

    private val windowManager =
        composeView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val displayWidth: Int
        get() = context.resources.displayMetrics.widthPixels

    private val params: WindowManager.LayoutParams =
        WindowManager.LayoutParams().apply {
            // Position bottom sheet from the bottom of the screen
            gravity = Gravity.BOTTOM or Gravity.START
            // Application panel window
            type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
            // Fill up the entire app view
            width = displayWidth
            height = WindowManager.LayoutParams.MATCH_PARENT

            // Format of screen pixels
            format = PixelFormat.TRANSLUCENT
            // Title used as fallback for a11y services
            // TODO: Provide bottom sheet window resource
            title = composeView.context.resources.getString(
                androidx.compose.ui.R.string.default_popup_window_title,
            )
            // Get the Window token from the parent view
            token = composeView.applicationWindowToken

            // Flags specific to modal bottom sheet.
            flags = flags and (
                WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                ).inv()

            flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            // Security flag
            val secureFlagEnabled =
                properties.securePolicy.shouldApplySecureFlag(composeView.isFlagSecureEnabled())
            if (secureFlagEnabled) {
                flags = flags or WindowManager.LayoutParams.FLAG_SECURE
            } else {
                flags = flags and (WindowManager.LayoutParams.FLAG_SECURE.inv())
            }

            // Focusable
            if (!properties.isFocusable) {
                flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            } else {
                flags = flags and (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv())
            }
        }

    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    @Composable
    override fun Content() {
        CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides this) {
            content()
        }
    }

    fun setCustomContent(
        parent: CompositionContext? = null,
        content: @Composable () -> Unit,
    ) {
        parent?.let { setParentCompositionContext(it) }
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
    }

    fun show() {
        windowManager.addView(this, params)
        if (isAtLeastT) {
            onBackPressedDispatcher.setOnBackInvokedDispatcher(findOnBackInvokedDispatcher()!!)
        }
    }

    fun dismiss() {
        setViewTreeLifecycleOwner(null)
        setViewTreeSavedStateRegistryOwner(null)
        composeView.viewTreeObserver.removeOnGlobalLayoutListener(this)
        windowManager.removeViewImmediate(this)
    }

    override fun onGlobalLayout() {
        // No-op
    }

    override fun setLayoutDirection(layoutDirection: Int) {
        // Do nothing. ViewRootImpl will call this method attempting to set the layout direction
        // from the context's locale, but we have one already from the parent composition.
    }

    // Sets the "real" layout direction for our content that we obtain from the parent composition.
    fun superSetLayoutDirection(layoutDirection: LayoutDirection) {
        val direction = when (layoutDirection) {
            LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
            LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
        }
        super.setLayoutDirection(direction)
    }
}

// Taken from AndroidPopup.android.kt
private fun View.isFlagSecureEnabled(): Boolean {
    val windowParams = rootView.layoutParams as? WindowManager.LayoutParams
    if (windowParams != null) {
        return (windowParams.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
    }
    return false
}

// Taken from AndroidPopup.android.kt
private fun SecureFlagPolicy.shouldApplySecureFlag(isSecureFlagSetOnParent: Boolean): Boolean {
    return when (this) {
        SecureFlagPolicy.SecureOff -> false
        SecureFlagPolicy.SecureOn -> true
        SecureFlagPolicy.Inherit -> isSecureFlagSetOnParent
    }
}
