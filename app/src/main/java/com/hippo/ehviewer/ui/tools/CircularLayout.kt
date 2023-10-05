package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val FULL_CIRCLE: Float = (PI * 2).toFloat()
private const val QUARTER_CIRCLE = PI / 2

@Composable
fun CircularLayout(
    modifier: Modifier = Modifier,
    placeFirstItemInCenter: Boolean = false,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->
        val radiusPx = constraints.maxWidth * 0.4
        val itemConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { measurable -> measurable.measure(itemConstraints) }
        val outPlaceables = if (placeFirstItemInCenter) placeables.drop(1) else placeables
        val theta = FULL_CIRCLE / outPlaceables.count()

        layout(
            width = constraints.minWidth,
            height = constraints.minHeight,
        ) {
            if (placeFirstItemInCenter) {
                placeables[0].let {
                    val centerOffsetX = constraints.maxWidth / 2 - it.width / 2
                    val centerOffsetY = constraints.maxHeight / 2 - it.height / 2
                    it.place(
                        x = centerOffsetX,
                        y = centerOffsetY,
                    )
                }
            }
            outPlaceables.forEachIndexed { i, it ->
                val centerOffsetX = constraints.maxWidth / 2 - it.width / 2
                val centerOffsetY = constraints.maxHeight / 2 - it.height / 2
                val offsetX = radiusPx * cos(theta * i - QUARTER_CIRCLE) + centerOffsetX
                val offsetY = radiusPx * sin(theta * i - QUARTER_CIRCLE) + centerOffsetY
                it.place(
                    x = offsetX.roundToInt(),
                    y = offsetY.roundToInt(),
                )
            }
        }
    }
}
