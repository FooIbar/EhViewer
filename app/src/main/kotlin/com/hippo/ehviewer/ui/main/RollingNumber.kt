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
        val from = transition.currentState
        val fromLen = from.length
        val to = transition.targetState
        val toLen = to.length
        val max = max(fromLen, toLen)
        repeat(max) { where ->
            val rotate by transition.animateOffset { str ->
                val len = str.length
                val absent = max - len
                val v = if (where < absent) null else str[where - absent].digitToInt()
                mapToOffset(v)
            }
            Column(
                modifier = Modifier.offset {
                    val degree = atan2(rotate.y, rotate.x)
                    val normalized = normalize(degree - zeroDegree)
                    println(normalized / gap)
                    val number = if (normalized > 9 * gap) {
                        val reNormalized = normalized - 9 * gap - gap / 2
                        if (reNormalized > 0) {
                            // 9 side
                            9 + reNormalized / gap * 2
                        } else {
                            // 0 side
                            reNormalized / gap * 2
                        }
                    } else {
                        normalized / gap
                    }
                    numberToOffset(size, number)
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = " ",
                    style = style,
                )
                repeat(10) { i ->
                    Text(
                        text = "$i",
                        style = style,
                    )
                }
                Text(
                    text = " ",
                    style = style,
                )
            }
        }
    }
}

private const val gap = (2 * PI / 10).toFloat()
private const val zeroDegree = - (PI / 2 + gap / 2).toFloat()
private const val factor = 10f

private fun mapToOffset(value: Int?): Offset {
    if (value != null) {
        val theta = zeroDegree + value * gap
        return Offset(cos(theta), sin(theta)) * factor
    } else {
        return Offset(0f, -2f) * factor
    }
}

private fun normalize(degree: Float) = (degree - 2 * PI * floor(degree / (2 * PI))).toFloat()

private fun numberToOffset(size: IntSize, number: Float): IntOffset = IntOffset(0, -(size.height * (number + 1)).toInt())
