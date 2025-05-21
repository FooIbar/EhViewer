package com.hippo.ehviewer.ui.tools

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderDefaults.drawStopIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastRoundToInt

@Composable
fun Slider(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    steps: Int = valueRange.last - valueRange.first - 1,
    maxTickCount: Int = defaultMaxTickCount(),
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.fastRoundToInt()) },
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        steps = steps,
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                trackCornerSize = Dp.Unspecified,
                enabled = enabled,
                colors = colors,
                drawTick = if (steps < maxTickCount) {
                    { offset, color ->
                        drawStopIndicator(offset = offset, color = color, size = SliderDefaults.TickSize)
                    }
                } else {
                    NoOpDrawTick
                },
            )
        },
        valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
    )
}

@Composable
fun defaultMaxTickCount() = with(LocalDensity.current) {
    LocalWindowInfo.current.containerSize.width / (SliderDefaults.TickSize.toPx() * 2.5f).toInt()
}

private val NoOpDrawTick: DrawScope.(Offset, Color) -> Unit = { _, _ -> }
