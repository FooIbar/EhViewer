package com.jamal.composeprefs3.ui.prefs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.ui.main.RollingNumber
import com.hippo.ehviewer.ui.tools.Slider

@Composable
fun SliderPref(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    defaultValue: Int = 0,
    onValueChangeFinished: ((Int) -> Unit)? = null,
    valueRange: IntRange = 0..1,
    showValue: Boolean = false,
    steps: Int = 0,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    display: (Int) -> Int = { it },
) {
    var value by remember { mutableIntStateOf(defaultValue) }
    Column(
        verticalArrangement = Arrangement.Center,
    ) {
        TextPref(
            title = title,
            modifier = modifier,
            summary = summary,
            textColor = textColor,
            minimalHeight = true,
            leadingIcon = leadingIcon,
            enabled = enabled,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Slider(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.weight(2.1f).padding(start = 16.dp, end = 16.dp),
                valueRange = valueRange,
                steps = steps,
                onValueChangeFinished = { onValueChangeFinished?.invoke(value) },
                enabled = enabled,
            )
            if (showValue) {
                RollingNumber(
                    number = display(value),
                    color = textColor,
                    modifier = Modifier.weight(0.5f),
                )
            }
        }
    }
}
