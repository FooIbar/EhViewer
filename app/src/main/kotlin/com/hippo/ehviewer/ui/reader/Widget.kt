package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.ui.tools.Slider

@Composable
fun SpinnerChoice(title: String, entries: Array<String>, values: Array<String>, field: MutableState<Int>) {
    var value by field
    var dropdown by remember { mutableStateOf(false) }
    val data = remember { entries zip values.map { v -> v.toInt() } }
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp).clickable { dropdown = true }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, modifier = Modifier.weight(0.5f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.weight(0.5f)) {
            Text(text = data.firstNotNullOf { (k, v) -> k.takeIf { v == value } }, modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            DropdownMenu(expanded = dropdown, onDismissRequest = { dropdown = false }) {
                data.forEach { (k, v) ->
                    DropdownMenuItem(
                        text = { Text(text = k) },
                        onClick = {
                            dropdown = false
                            value = v
                        },
                        modifier = Modifier.width(192.dp),
                        leadingIcon = {
                            if (value == v) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SwitchChoice(title: String, field: MutableState<Boolean>) {
    var value by field
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp).clickable { value = !value }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Switch(
            checked = value,
            onCheckedChange = { value = !value },
        )
    }
}

@Composable
fun SliderChoice(
    startSlot: @Composable () -> Unit,
    endSlot: @Composable () -> Unit,
    range: IntRange,
    field: MutableState<Int>,
) {
    var value by field
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        startSlot()
        Slider(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.weight(1f).padding(8.dp),
            valueRange = range,
        )
        endSlot()
    }
}
