package eu.kanade.tachiyomi.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.ui.tools.HapticFeedbackType
import com.hippo.ehviewer.ui.tools.LocalWindowSizeClass
import com.hippo.ehviewer.ui.tools.isExpanded
import com.hippo.ehviewer.ui.tools.rememberHapticFeedback
import kotlin.math.roundToInt

@Composable
fun ChapterNavigator(
    isRtl: Boolean,
    currentPage: Int,
    totalPages: Int,
    onSliderValueChange: (Int) -> Unit,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val horizontalPadding = if (windowSizeClass.isExpanded) 24.dp else 16.dp
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val configuration = LocalConfiguration.current
    val maxTickCount = configuration.screenWidthDp / (SliderDefaults.TickSize.value * 2.5f).roundToInt()
    val showTicks = totalPages < maxTickCount

    // We explicitly handle direction based on the reader viewer rather than the system direction
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (totalPages > 1) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Row(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(toolbarColor).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = currentPage.toString())

                        val interactionSource = remember { MutableInteractionSource() }
                        if (showTicks) {
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
                            value = currentPage.toFloat(),
                            valueRange = 1f..totalPages.toFloat(),
                            steps = totalPages - 2,
                            onValueChange = {
                                onSliderValueChange(it.roundToInt() - 1)
                            },
                            interactionSource = interactionSource,
                            colors = if (showTicks) {
                                SliderDefaults.colors()
                            } else {
                                SliderDefaults.colors(
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent,
                                )
                            },
                        )

                        Text(text = totalPages.toString())
                    }
                }
            }
        }
    }
}
