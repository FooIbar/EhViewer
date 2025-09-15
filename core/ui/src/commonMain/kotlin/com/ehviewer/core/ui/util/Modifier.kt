package com.ehviewer.core.ui.util

import androidx.compose.ui.Modifier

inline fun Modifier.thenIf(condition: Boolean, crossinline block: Modifier.() -> Modifier) = if (condition) block() else this
