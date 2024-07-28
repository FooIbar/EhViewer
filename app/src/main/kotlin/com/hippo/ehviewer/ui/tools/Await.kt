package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import arrow.core.None
import arrow.core.Option
import arrow.core.some

@Composable
fun <T> Await(
    block: suspend () -> T,
    placeholder: (@Composable () -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    var completed by remember(block) { mutableStateOf<Option<T>>(None) }
    LaunchedEffect(block) {
        completed = block().some()
    }
    if (placeholder != null) {
        completed.onNone { placeholder() }
    }
    completed.onSome { content(it) }
}

@Composable
fun <T> Await(key: Any?, block: suspend () -> T, content: @Composable (T) -> Unit) {
    var completed by remember(key, block) { mutableStateOf<Option<T>>(None) }
    LaunchedEffect(key, block) {
        completed = block().some()
    }
    completed.onSome { content(it) }
}
