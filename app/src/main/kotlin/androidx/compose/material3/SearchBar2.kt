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
@file:Suppress("ktlint:standard:property-naming")

package androidx.compose.material3

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldLineLimits
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.material3.SearchBarDefaults.InputFieldHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * <a href="https://m3.material.io/components/search/overview" class="external" target="_blank">Material Design search</a>.
 *
 * A search bar represents a floating search field that allows users to enter a keyword or phrase
 * and get relevant information. It can be used as a way to navigate through an app via search
 * queries.
 *
 * An active search bar expands into a search "view" and can be used to display dynamic suggestions.
 *
 * ![Search bar image](https://developer.android.com/images/reference/androidx/compose/material3/search-bar.png)
 *
 * A [SearchBar] expands to occupy the entirety of its allowed size when active. For full-screen
 * behavior as specified by Material guidelines, parent layouts of the [SearchBar] must not pass
 * any [Constraints] that limit its size, and the host activity should set
 * `WindowCompat.setDecorFitsSystemWindows(window, false)`.
 *
 * If this expansion behavior is undesirable, for example on large tablet screens, [DockedSearchBar]
 * can be used instead.
 *
 * @param state the query text to be shown in the search bar's input field
 * @param onSearch the callback to be invoked when the input service triggers the [ImeAction.Search]
 * action.
 * @param active whether this search bar is active
 * @param onActiveChange the callback to be invoked when this search bar's active state is changed
 * @param modifier the [Modifier] to be applied to this search bar
 * @param enabled controls the enabled state of this search bar. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param placeholder the placeholder to be displayed when the search bar's query is empty.
 * @param leadingIcon the leading icon to be displayed at the beginning of the search bar container
 * @param trailingIcon the trailing icon to be displayed at the end of the search bar container
 * @param shape the shape of this search bar when it is not [active]. When [active], the shape will
 * always be [SearchBarDefaults.fullScreenShape].
 * @param colors [SearchBarColors] that will be used to resolve the colors used for this search bar
 * in different states. See [SearchBarDefaults.colors].
 * @param tonalElevation when [SearchBarColors.containerColor] is [ColorScheme.surface], a
 * translucent primary color overlay is applied on top of the container. A higher tonal elevation
 * value will result in a darker color in light theme and lighter color in dark theme. See also:
 * [Surface].
 * @param shadowElevation the elevation for the shadow below the search bar
 * @param windowInsets the window insets that the search bar will respect
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this search bar. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this search bar in different states.
 * @param content the content of this search bar that will be displayed below the input field
 */
@ExperimentalMaterial3Api
@Composable
fun SearchBar(
    state: TextFieldState,
    onSearch: () -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = SearchBarDefaults.inputFieldShape,
    colors: SearchBarColors = SearchBarDefaults.colors(),
    tonalElevation: Dp = SearchBarDefaults.TonalElevation,
    shadowElevation: Dp = SearchBarDefaults.ShadowElevation,
    windowInsets: WindowInsets = SearchBarDefaults.windowInsets,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit,
) {
    val animationProgress = remember { Animatable(initialValue = if (active) 1f else 0f) }
    var firstBackEvent by remember { mutableStateOf<BackEventCompat?>(null) }
    var currentBackEvent by remember { mutableStateOf<BackEventCompat?>(null) }

    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    val defaultInputFieldShape = SearchBarDefaults.inputFieldShape
    val defaultFullScreenShape = SearchBarDefaults.fullScreenShape
    val useFullScreenShape by remember {
        derivedStateOf(structuralEqualityPolicy()) { animationProgress.value == 1f }
    }
    val animatedShape = remember(useFullScreenShape, shape) {
        when {
            shape == defaultInputFieldShape ->
                // The shape can only be animated if it's the default spec value
                GenericShape { size, _ ->
                    val radius = with(density) {
                        (SearchBarCornerRadius * (1 - animationProgress.value)).toPx()
                    }
                    addRoundRect(RoundRect(size.toRect(), CornerRadius(radius)))
                }
            useFullScreenShape -> defaultFullScreenShape
            else -> shape
        }
    }

    // The main animation complexity is allowing the component to smoothly expand while keeping the
    // input field at the same relative location on screen. `Modifier.windowInsetsPadding` does not
    // support animation and thus is not suitable. Instead, we convert the insets to a padding
    // applied to the Surface, which gradually becomes padding applied to the input field as the
    // animation proceeds.
    val unconsumedInsets = remember { MutableWindowInsets() }
    val topPadding = remember(density) {
        derivedStateOf {
            SearchBarVerticalPadding +
                unconsumedInsets.asPaddingValues(density).calculateTopPadding()
        }
    }

    Surface(
        shape = animatedShape,
        color = colors.containerColor,
        contentColor = contentColorFor(colors.containerColor),
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        modifier = modifier
            .padding(horizontal = lerp(SearchBarHorizontalPadding, 0.dp, animationProgress.value))
            .zIndex(1f)
            .onConsumedWindowInsetsChanged { consumedInsets ->
                unconsumedInsets.insets = windowInsets.exclude(consumedInsets)
            }
            .consumeWindowInsets(unconsumedInsets)
            .layout { measurable, constraints ->
                val animatedTopPadding =
                    lerp(topPadding.value, 0.dp, animationProgress.value).roundToPx()

                val startWidth: Int
                val startHeight: Int
                if (currentBackEvent == null) {
                    startWidth = max(constraints.minWidth, SearchBarMinWidth.roundToPx())
                        .coerceAtMost(min(constraints.maxWidth, SearchBarMaxWidth.roundToPx()))
                    startHeight = max(constraints.minHeight, InputFieldHeight.roundToPx())
                        .coerceAtMost(constraints.maxHeight)
                } else {
                    startWidth =
                        (constraints.maxWidth * SearchBarPredictiveBackMinScale).roundToInt()
                    startHeight =
                        (constraints.maxHeight * SearchBarPredictiveBackMinScale).roundToInt()
                }
                val endWidth = constraints.maxWidth
                val endHeight = constraints.maxHeight

                val width = lerp(startWidth, endWidth, animationProgress.value)
                val height =
                    lerp(startHeight, endHeight, animationProgress.value) + animatedTopPadding

                val minOffsetMargin = SearchBarPredictiveBackMinMargin.roundToPx()
                val predictiveBackOffsetX = calculatePredictiveBackOffsetX(
                    constraints,
                    minOffsetMargin,
                    currentBackEvent,
                    animationProgress.value,
                )
                val predictiveBackOffsetY = calculatePredictiveBackOffsetY(
                    constraints,
                    minOffsetMargin,
                    currentBackEvent,
                    firstBackEvent,
                    height,
                    SearchBarPredictiveBackMaxOffsetY.roundToPx(),
                )
                val placeable = measurable.measure(
                    Constraints
                        .fixed(width, height)
                        .offset(
                            vertical = -animatedTopPadding,
                        ),
                )
                layout(width, height) {
                    placeable.placeRelative(
                        predictiveBackOffsetX,
                        animatedTopPadding + predictiveBackOffsetY,
                    )
                }
            },
    ) {
        Column {
            val animatedInputFieldPadding = remember {
                AnimatedPaddingValues2(animationProgress.asState(), topPadding)
            }
            SearchBarInputField(
                state = state,
                onSearch = onSearch,
                onActiveChange = onActiveChange,
                modifier = Modifier.padding(paddingValues = animatedInputFieldPadding),
                enabled = enabled,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                colors = colors.inputFieldColors,
                interactionSource = interactionSource,
            )

            val showResults by remember {
                derivedStateOf(structuralEqualityPolicy()) { animationProgress.value > 0 }
            }
            if (showResults) {
                Column(Modifier.graphicsLayer { alpha = animationProgress.value }) {
                    HorizontalDivider(color = colors.dividerColor)
                    content()
                }
            }
        }
    }

    val isFocused = interactionSource.collectIsFocusedAsState().value
    val shouldClearFocus = !active && isFocused
    if (shouldClearFocus) {
        LaunchedEffect(true) {
            // Not strictly needed according to the motion spec, but since the animation already has
            // a delay, this works around b/261632544.
            delay(AnimationDelayMillis.toLong())
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(active) {
        val animationInProgress = animationProgress.value > 0 && animationProgress.value < 1
        val animationSpec =
            if (animationInProgress) {
                AnimationPredictiveBackExitFloatSpec
            } else if (active) {
                AnimationEnterFloatSpec
            } else {
                AnimationExitFloatSpec
            }
        animationProgress.animateTo(
            targetValue = if (active) 1f else 0f,
            animationSpec = animationSpec,
        )
    }
    val mutatorMutex = remember { MutatorMutex() }
    PredictiveBackHandler(enabled = active) { progress ->
        mutatorMutex.mutate {
            var canceled = false
            try {
                progress.collect { backEvent ->
                    if (firstBackEvent == null) {
                        firstBackEvent = backEvent
                    }
                    currentBackEvent = backEvent
                    val interpolatedProgress = EaseOut.transform(backEvent.progress)
                    animationProgress.snapTo(targetValue = 1 - interpolatedProgress)
                }
            } catch (e: CancellationException) {
                canceled = true
            } finally {
                firstBackEvent = null
                currentBackEvent = null
                onActiveChange(canceled)
            }
        }
    }
}

private fun calculatePredictiveBackOffsetX(
    constraints: Constraints,
    minMargin: Int,
    currentBackEvent: BackEventCompat?,
    progress: Float,
): Int {
    if (currentBackEvent == null) {
        return 0
    }
    val directionMultiplier = if (currentBackEvent.swipeEdge == BackEventCompat.EDGE_LEFT) 1 else -1
    val maxOffsetX =
        (constraints.maxWidth * SearchBarPredictiveBackMaxOffsetXRatio) - minMargin
    return (maxOffsetX * (1 - progress)).toInt() * directionMultiplier
}

private fun calculatePredictiveBackOffsetY(
    constraints: Constraints,
    minMargin: Int,
    currentBackEvent: BackEventCompat?,
    firstBackEvent: BackEventCompat?,
    height: Int,
    maxOffsetY: Int,
): Int {
    if (firstBackEvent == null || currentBackEvent == null) {
        return 0
    }
    val availableVerticalSpace = max(0, (constraints.maxHeight - height) / 2 - minMargin)
    val adjustedMaxOffsetY = min(availableVerticalSpace, maxOffsetY)
    val yDelta = currentBackEvent.touchY - firstBackEvent.touchY
    val yProgress = abs(yDelta) / constraints.maxHeight
    val directionMultiplier = sign(yDelta)
    return (lerp(0, adjustedMaxOffsetY, yProgress) * directionMultiplier).roundToInt()
}

@Composable
private fun SearchBarInputField(
    state: TextFieldState,
    onSearch: () -> Unit,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: TextFieldColors = SearchBarDefaults.inputFieldColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focusRequester = remember { FocusRequester() }
    val focused by interactionSource.collectIsFocusedAsState()
    val textColor = LocalTextStyle.current.color.takeOrElse {
        if (focused) colors.focusedTextColor else colors.unfocusedTextColor
    }

    BasicTextField2(
        state = state,
        modifier = modifier
            .height(InputFieldHeight)
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onActiveChange(true) }
            .semantics {
                onClick {
                    focusRequester.requestFocus()
                    true
                }
            },
        enabled = enabled,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = LocalTextStyle.current.merge(TextStyle(color = textColor)),
        cursorBrush = SolidColor(colors.cursorColor),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        interactionSource = interactionSource,
        decorator = @Composable { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = state.text.toString(),
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = placeholder,
                leadingIcon = leadingIcon?.let { leading ->
                    {
                        Box(Modifier.offset(x = SearchBarIconOffsetX)) { leading() }
                    }
                },
                trailingIcon = trailingIcon?.let { trailing ->
                    {
                        Box(Modifier.offset(x = -SearchBarIconOffsetX)) { trailing() }
                    }
                },
                shape = SearchBarDefaults.inputFieldShape,
                colors = colors,
                contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(),
                container = {},
            )
        },
    )
}

@Stable
private class AnimatedPaddingValues2(
    val animationProgress: State<Float>,
    val topPadding: State<Dp>,
) : PaddingValues {
    override fun calculateTopPadding(): Dp = topPadding.value * animationProgress.value
    override fun calculateBottomPadding(): Dp = SearchBarVerticalPadding * animationProgress.value

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = 0.dp
    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = 0.dp
}

// Measurement specs
private val SearchBarCornerRadius: Dp = InputFieldHeight / 2
internal val SearchBarMinWidth: Dp = 360.dp
private val SearchBarMaxWidth: Dp = 720.dp
internal val SearchBarVerticalPadding: Dp = 8.dp
internal val SearchBarHorizontalPadding: Dp = 16.dp

// Search bar has 16dp padding between icons and start/end, while by default text field has 12dp.
private val SearchBarIconOffsetX: Dp = 4.dp

private const val SearchBarPredictiveBackMinScale: Float = 9f / 10f
private val SearchBarPredictiveBackMinMargin: Dp = 8.dp
private const val SearchBarPredictiveBackMaxOffsetXRatio: Float = 1f / 20f
private val SearchBarPredictiveBackMaxOffsetY: Dp = 24.dp

// Animation specs
private const val AnimationEnterDurationMillis: Int = 600
private const val AnimationExitDurationMillis: Int = 350
private const val AnimationDelayMillis: Int = 100
private val AnimationEnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val AnimationExitEasing = CubicBezierEasing(0.0f, 1.0f, 0.0f, 1.0f)
private val AnimationEnterFloatSpec: FiniteAnimationSpec<Float> = tween(
    durationMillis = AnimationEnterDurationMillis,
    delayMillis = AnimationDelayMillis,
    easing = AnimationEnterEasing,
)
private val AnimationExitFloatSpec: FiniteAnimationSpec<Float> = tween(
    durationMillis = AnimationExitDurationMillis,
    delayMillis = AnimationDelayMillis,
    easing = AnimationExitEasing,
)
private val AnimationPredictiveBackExitFloatSpec: FiniteAnimationSpec<Float> = tween(
    durationMillis = AnimationExitDurationMillis,
    easing = AnimationExitEasing,
)
