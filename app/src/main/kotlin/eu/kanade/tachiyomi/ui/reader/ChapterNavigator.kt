package eu.kanade.tachiyomi.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.ui.tools.HapticFeedbackType
import com.hippo.ehviewer.ui.tools.LocalWindowSizeClass
import com.hippo.ehviewer.ui.tools.Slider
import com.hippo.ehviewer.ui.tools.defaultMaxTickCount
import com.hippo.ehviewer.ui.tools.isExpanded
import com.hippo.ehviewer.ui.tools.rememberHapticFeedback

@Composable
fun ChapterNavigator(
    isRtl: Boolean,
    currentPage: Int,
    totalPages: Int,
    onSliderValueChange: (Int) -> Unit,
) = CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
    val windowSizeClass = LocalWindowSizeClass.current
    val horizontalPadding = if (windowSizeClass.isExpanded) 24.dp else 16.dp
    Row(
        modifier = Modifier.padding(horizontal = horizontalPadding).clip(CircleShape).background(toolbarColor).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "$currentPage")
        val steps = totalPages - 2
        val maxTickCount = defaultMaxTickCount()
        val interactionSource = remember { MutableInteractionSource() }
        if (steps < maxTickCount) {
            val sliderDragged by interactionSource.collectIsDraggedAsState()
            val hapticFeedback = rememberHapticFeedback()
            LaunchedEffect(currentPage) {
                if (sliderDragged) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.MOVE)
                }
            }
        }
        Slider(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            value = currentPage,
            valueRange = 1..totalPages,
            steps = steps,
            onValueChange = onSliderValueChange,
            maxTickCount = maxTickCount,
            interactionSource = interactionSource,
        )
        Text(text = "$totalPages")
    }
}
