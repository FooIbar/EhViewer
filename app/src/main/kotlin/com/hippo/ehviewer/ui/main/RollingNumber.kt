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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

@Composable
fun RollingNumber(number: Int, style: TextStyle = LocalTextStyle.current) {
    val string = remember(number) { "$number" }
    val transition = updateTransition(string)
    val textMeasurer = rememberTextMeasurer()
    val size by transition.animateIntSize { str ->
        remember(str) { textMeasurer.measure(str, style).size }
    }
    Row(
        modifier = Modifier.clipToBounds().layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(width = placeable.width, height = size.height) {
                placeable.place(0, 0)
            }
        },
        horizontalArrangement = Arrangement.End,
    ) {
        val content = remember {
            val meta = @Composable { reversed: Int ->
                val max = with(transition) { max(currentState.length, targetState.length) }
                val rotate by transition.animateOffset { str ->
                    val len = str.length
                    val absent = max - len
                    val where = max - reversed - 1
                    val v = if (where < absent) null else str[where - absent].digitToInt()
                    mapSimplyConnectedElementToEuclideanPartialCircle(v)
                }
                Column(
                    modifier = Modifier.offset {
                        val number = partialCircleToLinearUIOffset(rotate)
                        numberToOffset(size, 9 - number)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    repeat(10) { i ->
                        Text(
                            text = "${9 - i}",
                            style = style,
                        )
                        Text(
                            text = " ",
                            style = style,
                        )
                    }
                }
            }
            (0 until maxNumber).map {
                movableContentOf { meta(it) }
            }
        }
        val max = with(transition) { max(currentState.length, targetState.length) }
        check(max <= maxNumber)
        repeat(max) { index ->
            content[max - index - 1]()
        }
    }
}

private const val maxNumber = 5

// This factor controls animation speed
private const val factor = 10f

private const val gap = (2 * PI / 10).toFloat()
private const val zeroDegree = -(PI / 2 + gap / 2).toFloat()

// A special factor depends on gaps to keep 0 ~ null, null ~ 9 have same distance
private const val specialNodeDistanceFactor = 1.528f

// Convert a number which belongs **1-connected** animation space to 2d euclidean space
// Input 1-connected space: 0 1 2 3 4 6 6 7 8 9 null. where 0 is connected to null, so **1-connected**
// Output: partial circle in 2d euclidean space, the radius is controlled by [factor]
// but null is always mapped to (0, 2 * factor), and keep same norm (distance) with 0 and 9
private fun mapSimplyConnectedElementToEuclideanPartialCircle(value: Int?): Offset {
    if (value != null) {
        val theta = zeroDegree - value * gap
        return Offset(cos(theta), sin(theta)) * factor
    } else {
        return Offset(0f, -specialNodeDistanceFactor) * factor
    }
}

// Output: -1 ~ 10, where -1 and 10 shows nothing
private fun partialCircleToLinearUIOffset(circleOffset: Offset): Float {
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
private fun numberToOffset(size: IntSize, number: Float) = IntOffset(0, -(size.height * 2 * number).toInt())
