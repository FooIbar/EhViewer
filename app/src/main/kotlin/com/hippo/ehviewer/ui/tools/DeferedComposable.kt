package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun <T> Deferred(block: suspend () -> T, content: @Composable (T) -> Unit) {
    var completed by remember(block) { mutableStateOf<T?>(null) }
    LaunchedEffect(block) {
        completed = block()
    }
    completed?.let { content(it) }
}

@Composable
fun <T> Deferred(key: Any?, block: suspend () -> T, content: @Composable (T) -> Unit) {
    var completed by remember(key, block) { mutableStateOf<T?>(null) }
    LaunchedEffect(key, block) {
        completed = block()
    }
    completed?.let { content(it) }
}
