@file:Suppress("ktlint:standard:property-naming")

package com.hippo.ehviewer.ui.main

import androidx.compose.animation.core.animateIntSize
import androidx.compose.animation.core.animateOffset
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.util.unsafeLazy
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

// We have three space to make animation looks continuous
// 1. Concept space, where we have elements 0 1 2 3 4 5 6 7 8 9 null
//    0, 9, null is connected and the have same norm(distance) to each other
// 2. Intermediate, which is 2d euclidean space
//    0 ~ 9 in Concept space is mapped to a circle rigidly
//    null is mapped to a special point(0, - 1.528f * radius)
//    keeping null - 0, null - 9, 0 - 9 have same distance
// 3. Number Offset Space
//    value is from -1 ~ 10, where -1 and 10 shows nothing
//    radian of point in Intermediate space is mapped to here discontinuously

// This controls animation speed
private const val radius = 100f

// Gaps between number in radian
private const val gap = (2 * PI / 10).toFloat()
private const val zeroDegree = -(PI / 2 - gap / 2).toFloat()

// null is mapped to (0, - 1.528f * radius)
private const val specialNodeDistanceFactor = 1.528f

// Input: Concept space: 0 null 1 2 3 4 6 6 7 8 9
// Output: Position in 2d euclidean space partial circle
private fun conceptSpaceToIntermediate(value: Int?): Offset {
    if (value != null) {
        val theta = zeroDegree - value * gap
        return Offset(cos(theta), sin(theta)) * radius
    } else {
        return Offset(0f, -specialNodeDistanceFactor) * radius
    }
}

// Output: -1 ~ 10, where -1 and 10 shows nothing
private fun intermediateToNumberOffset(circleOffset: Offset): Float {
    val degree = atan2(circleOffset.y, circleOffset.x)
    val normalized = normalize(zeroDegree - degree)
    return if (normalized > 9 * gap) {
        val reNormalized = normalized - 9 * gap - gap / 2
        if (reNormalized > 0) {
            // 0 side
            reNormalized / gap * 2 - 1
        } else {
            // 9 side
            10 + reNormalized / gap * 2
        }
    } else {
        normalized / gap
    }
}

// Normalize radian to [0, 2 * PI]
private fun normalize(degree: Float) = (degree - 2 * PI * floor(degree / (2 * PI))).toFloat()

// Convert 2d euclidean space partial circle to UI Offset
private fun numberOffsetToUIOffset(size: IntSize, number: Float) = IntOffset(0, -(size.height * 2 * number).toInt())

@Composable
fun RollingNumber(number: Int, style: TextStyle = LocalTextStyle.current, width: Int? = null) {
    val string = remember(number) { "$number" }
    val transition = updateTransition(string)
    val textMeasurer = rememberTextMeasurer()
    val size by transition.animateIntSize { str ->
        remember(str) { textMeasurer.measure(str, style).size }
    }
    val density = LocalDensity.current
    val (styleNoSpacing, spacing) = remember(density, style) {
        if (style.letterSpacing != TextUnit.Unspecified) {
            val spacing = with(density) { style.letterSpacing.toDp() }
            style.copy(letterSpacing = TextUnit.Unspecified) to spacing
        } else {
            style to 0.dp
        }
    }
    Row(
        modifier = Modifier.clipToBounds().layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(width = placeable.width, height = size.height) {
                placeable.place(0, 0)
            }
        },
        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.End),
    ) {
        val content = remember {
            val meta = @Composable { reversed: Int ->
                val max = width ?: with(transition) { max(currentState.length, targetState.length) }
                val rotate by transition.animateOffset { str ->
                    val len = str.length
                    val absent = max - len
                    val where = max - reversed - 1
                    val v = if (where < absent) null else str[where - absent].digitToInt()
                    conceptSpaceToIntermediate(v)
                }
                Column(
                    modifier = Modifier.offset {
                        val number = intermediateToNumberOffset(rotate)
                        numberOffsetToUIOffset(size, number)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    repeat(10) { i ->
                        Text(
                            text = "$i",
                            style = styleNoSpacing,
                        )
                        Text(
                            text = "",
                            style = styleNoSpacing,
                        )
                    }
                }
            }
            (0 until maxNumber).map {
                unsafeLazy {
                    if (width != null) {
                        { meta(it) }
                    } else {
                        movableContentOf { meta(it) }
                    }
                }
            }
        }
        val max = width ?: with(transition) { max(currentState.length, targetState.length) }
        check(max <= maxNumber)
        repeat(max) { index ->
            content[max - index - 1].value()
        }
    }
}

private const val maxNumber = 16
