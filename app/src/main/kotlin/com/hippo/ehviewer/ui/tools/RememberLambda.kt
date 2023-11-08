package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.cache
import androidx.compose.runtime.currentComposer

@Composable
fun rememberLambda(calculation: @DisallowComposableCalls () -> Unit): () -> Unit =
    currentComposer.cache(false) { calculation }

@Composable
fun rememberLambda(
    key1: Any?,
    calculation: @DisallowComposableCalls () -> Unit,
): () -> Unit {
    return currentComposer.cache(currentComposer.changed(key1)) { calculation }
}
