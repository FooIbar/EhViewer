package com.ehviewer.core.ui.util

import androidx.compose.runtime.Composable

enum class HapticFeedbackType {
    START,
    MOVE,
    END,
}

interface HapticFeedback {
    fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType)
}

@Composable
expect fun rememberHapticFeedback(): HapticFeedback
