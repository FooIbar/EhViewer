package com.hippo.ehviewer.ui.tools

import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.scrollbar.ScrollbarAdapter
import androidx.compose.foundation.scrollbar.ScrollbarStyle
import androidx.compose.foundation.scrollbar.VerticalScrollbar
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

@Composable
fun VerticalScrollbar(
    adapter: ScrollbarAdapter,
    isScrollInProgress: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
    reverseLayout: Boolean = false,
) {
    val isScrolling by rememberUpdatedState(isScrollInProgress)
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scrollbarAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { isDragged || isHovered || isScrolling }.collectLatest {
            if (it) {
                scrollbarAlpha.animateTo(1f, FadeInAnimationSpec)
            } else {
                scrollbarAlpha.animateTo(0f, FadeOutAnimationSpec)
            }
        }
    }
    val scrollbarModifier = if (scrollbarAlpha.value > 0f && !isDragged && !isScrollInProgress) {
        modifier.systemGestureExclusion()
    } else {
        modifier
    }
    VerticalScrollbar(
        adapter = adapter,
        modifier = scrollbarModifier
            .padding(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            )
            .graphicsLayer { alpha = scrollbarAlpha.value },
        reverseLayout = reverseLayout,
        interactionSource = interactionSource,
    )
}

fun scrollbarStyle(color: Color) = ScrollbarStyle(
    minimalHeight = ThumbLength,
    thickness = ThumbThickness,
    padding = ThumbPadding,
    shape = ThumbShape,
    hoverDurationMillis = 300,
    unhoverColor = color,
    hoverColor = color,
)

private val ThumbPadding = 8.dp
private val ThumbLength = 48.dp
private val ThumbThickness = 8.dp
private val ThumbShape = RoundedCornerShape(ThumbThickness / 2)
private val FadeOutAnimationSpec = tween<Float>(
    durationMillis = ViewConfiguration.getScrollBarFadeDuration(),
    delayMillis = 2000,
)
private val FadeInAnimationSpec = tween<Float>(
    durationMillis = ViewConfiguration.getScrollBarFadeDuration(),
)
