package com.hippo.ehviewer.ui.main

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntSize
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import kotlin.math.max

@Composable
fun RollingNumber(number: Int, style: TextStyle = LocalTextStyle.current) {
    val string = remember(number) { "$number" }
    val transition = updateTransition(string)
    val textMeasurer = rememberTextMeasurer()
    val size by transition.animateIntSize { str ->
        remember(str) { textMeasurer.measure(str, style).size }
    }
    Row(
        modifier = Modifier.clip(RectangleShape).layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(width = placeable.width, height = size.height) {
                placeable.place(0, 0)
            }
        }.wrapContentWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        val from = transition.currentState
        val fromLen = from.length
        val to = transition.targetState
        val toLen = to.length
        val max = max(fromLen, toLen)
        repeat(max) { where ->
            val offset by transition.animateFloat { str ->
                val len = str.length
                val absent = max - len
                if (where < absent) 1f else str[where - absent].digitToInt().toFloat()
            }
            Column(
                modifier = Modifier.offset {
                    IntOffset(0, -(size.height * offset).toInt())
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                repeat(10) { i ->
                    Text(
                        text = "$i",
                        style = style,
                    )
                }
            }
        }
    }
}
