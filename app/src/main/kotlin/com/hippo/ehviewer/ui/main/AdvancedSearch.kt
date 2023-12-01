package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.ui.tools.LabeledCheckbox
import com.hippo.ehviewer.util.toIntOrDefault

@Composable
fun SearchAdvanced(
    modifier: Modifier = Modifier,
    state: AdvancedSearchOption,
    onStateChanged: (AdvancedSearchOption) -> Unit,
) = FlowRow(modifier = modifier, horizontalArrangement = Arrangement.Center, maxItemsInEachRow = 2) {
    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
        val adv = state.advanceSearch
        fun checked(bit: Int) = adv and bit != 0
        fun AdvancedSearchOption.inv(bit: Int) = onStateChanged(copy(advanceSearch = advanceSearch xor bit))
        LabeledCheckbox(
            checked = checked(AdvanceTable.SH),
            onCheckedChange = { state.inv(AdvanceTable.SH) },
            label = stringResource(id = R.string.search_sh),
        )
        LabeledCheckbox(
            checked = checked(AdvanceTable.STO),
            onCheckedChange = { state.inv(AdvanceTable.STO) },
            label = stringResource(id = R.string.search_sto),
        )
        val minRatingItems = stringArrayResource(id = R.array.search_min_rating)
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.align(Alignment.CenterVertically).padding(12.dp),
        ) {
            val softwareKeyboardController = LocalSoftwareKeyboardController.current
            OutlinedTextField(
                modifier = Modifier.menuAnchor(),
                readOnly = true,
                value = minRatingItems[if (state.minRating in 2..5) state.minRating - 1 else 0],
                onValueChange = {},
                label = { Text(stringResource(id = R.string.search_sr)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                SideEffect {
                    softwareKeyboardController?.hide()
                }
                minRatingItems.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            expanded = false
                            val index = minRatingItems.indexOf(selectionOption)
                            onStateChanged(state.copy(minRating = if (index > 0) index + 1 else -1))
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            var enabled by rememberSaveable { mutableStateOf(false) }
            LabeledCheckbox(
                checked = enabled,
                onCheckedChange = { enabled = it },
                label = stringResource(id = R.string.search_sp),
            )
            OutlinedTextField(
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                value = if (enabled && state.fromPage != -1) state.fromPage.toString() else "",
                onValueChange = { onStateChanged(state.copy(fromPage = it.toIntOrDefault(-1))) },
                modifier = Modifier.width(96.dp).padding(16.dp),
                singleLine = true,
                enabled = enabled,
            )
            Text(text = stringResource(id = R.string.search_sp_to))
            OutlinedTextField(
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                value = if (enabled && state.toPage != -1) state.toPage.toString() else "",
                onValueChange = { onStateChanged(state.copy(toPage = it.toIntOrDefault(-1))) },
                modifier = Modifier.width(96.dp).padding(16.dp),
                singleLine = true,
                enabled = enabled,
            )
            Text(text = stringResource(id = R.string.search_sp_suffix))
        }
        Text(text = stringResource(id = R.string.search_sf), modifier = Modifier.align(Alignment.CenterVertically))
        FlowRow {
            LabeledCheckbox(
                checked = checked(AdvanceTable.SFL),
                onCheckedChange = { state.inv(AdvanceTable.SFL) },
                label = stringResource(id = R.string.search_sfl),
            )
            LabeledCheckbox(
                checked = checked(AdvanceTable.SFU),
                onCheckedChange = { state.inv(AdvanceTable.SFU) },
                label = stringResource(id = R.string.search_sfu),
            )
            LabeledCheckbox(
                checked = checked(AdvanceTable.SFT),
                onCheckedChange = { state.inv(AdvanceTable.SFT) },
                label = stringResource(id = R.string.search_sft),
            )
        }
    }
}
