package com.hippo.ehviewer.ui.tools

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.hippo.ehviewer.util.isAtLeastOMR1
import com.hippo.ehviewer.util.isAtLeastR
import com.hippo.ehviewer.util.isAtLeastU

enum class HapticFeedbackType {
    START,
    MOVE,
    END,
}

interface HapticFeedback {
    fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType)
}

@Composable
fun rememberHapticFeedback(): HapticFeedback {
    val view = LocalView.current
    return remember(view) { AndroidHapticFeedback(view) }
}

class AndroidHapticFeedback(private val view: View) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        val feedbackConstant = when (hapticFeedbackType) {
            HapticFeedbackType.START -> {
                if (isAtLeastU) {
                    HapticFeedbackConstants.DRAG_START
                } else if (isAtLeastR) {
                    HapticFeedbackConstants.GESTURE_START
                } else if (isAtLeastOMR1) {
                    HapticFeedbackConstants.KEYBOARD_PRESS
                } else {
                    HapticFeedbackConstants.VIRTUAL_KEY
                }
            }
            HapticFeedbackType.MOVE -> {
                if (isAtLeastU) {
                    HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
                } else if (isAtLeastOMR1) {
                    HapticFeedbackConstants.TEXT_HANDLE_MOVE
                } else {
                    HapticFeedbackConstants.CLOCK_TICK
                }
            }
            HapticFeedbackType.END -> {
                if (isAtLeastR) {
                    HapticFeedbackConstants.GESTURE_END
                } else if (isAtLeastOMR1) {
                    HapticFeedbackConstants.KEYBOARD_RELEASE
                } else {
                    HapticFeedbackConstants.VIRTUAL_KEY
                }
            }
        }
        view.performHapticFeedback(feedbackConstant)
    }
}
