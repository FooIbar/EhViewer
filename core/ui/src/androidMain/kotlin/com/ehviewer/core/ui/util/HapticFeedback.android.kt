package com.ehviewer.core.ui.util

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.ehviewer.core.util.isAtLeastOMR1
import com.ehviewer.core.util.isAtLeastR
import com.ehviewer.core.util.isAtLeastU

@Composable
actual fun rememberHapticFeedback(): HapticFeedback {
    val view = LocalView.current
    return remember(view) { AndroidHapticFeedback(view) }
}

class AndroidHapticFeedback(private val view: View) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        val feedbackConstant = when (hapticFeedbackType) {
            HapticFeedbackType.START -> when {
                isAtLeastU -> HapticFeedbackConstants.DRAG_START
                isAtLeastR -> HapticFeedbackConstants.GESTURE_START
                isAtLeastOMR1 -> HapticFeedbackConstants.KEYBOARD_PRESS
                else -> HapticFeedbackConstants.VIRTUAL_KEY
            }
            HapticFeedbackType.MOVE -> when {
                isAtLeastU -> HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
                isAtLeastOMR1 -> HapticFeedbackConstants.TEXT_HANDLE_MOVE
                else -> HapticFeedbackConstants.CLOCK_TICK
            }
            HapticFeedbackType.END -> when {
                isAtLeastR -> HapticFeedbackConstants.GESTURE_END
                isAtLeastOMR1 -> HapticFeedbackConstants.KEYBOARD_RELEASE
                else -> HapticFeedbackConstants.VIRTUAL_KEY
            }
        }
        view.performHapticFeedback(feedbackConstant)
    }
}
