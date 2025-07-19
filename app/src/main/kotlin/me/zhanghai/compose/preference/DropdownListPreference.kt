/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.zhanghai.compose.preference

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection

@Composable
fun <T> DropdownListPreference(
    state: MutableState<T>,
    items: Map<T, String>,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
) {
    val (value, onValueChange) = state
    DropdownListPreference(
        value = value,
        onValueChange = onValueChange,
        items = items,
        title = title,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        summary = summary,
    )
}

@Composable
fun <T> DropdownListPreference(
    value: T,
    onValueChange: (T) -> Unit,
    items: Map<T, String>,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
) {
    var openSelector by rememberSaveable { mutableStateOf(false) }
    // Put DropdownMenu before Preference so that it can anchor to the right position.
    if (openSelector) {
        val theme = LocalPreferenceTheme.current
        val padding = with(theme.padding) {
            val direction = LocalLayoutDirection.current
            PaddingValues(start = calculateStartPadding(direction), end = calculateStartPadding(direction))
        }
        Box(modifier = Modifier.fillMaxWidth().padding(padding)) {
            DropdownMenu(
                expanded = openSelector,
                onDismissRequest = { openSelector = false },
            ) {
                items.forEach {
                    DropdownMenuItem(
                        text = { Text(text = it.value) },
                        onClick = {
                            onValueChange(it.key)
                            openSelector = false
                        },
                        modifier = Modifier.background(
                            if (it.key == value) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                Color.Transparent
                            },
                        ),
                    )
                }
            }
        }
    }
    Preference(
        title = title,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        summary = summary,
    ) {
        openSelector = true
    }
}
