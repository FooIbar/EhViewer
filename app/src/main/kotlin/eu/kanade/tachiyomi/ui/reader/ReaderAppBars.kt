package eu.kanade.tachiyomi.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.reader.setting.OrientationType
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType

private val animationSpec = tween<IntOffset>(200)

@Composable
fun ReaderAppBars(
    visible: Boolean,
    isRtl: Boolean,
    showSeekBar: Boolean,
    currentPage: Int,
    totalPages: Int,
    onSliderValueChange: (Int) -> Unit,
    readingMode: ReadingModeType,
    onClickReadingMode: () -> Unit,
    orientationMode: OrientationType,
    onClickOrientationMode: () -> Unit,
    onClickSettings: () -> Unit,
) = Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
    Spacer(modifier = Modifier.weight(1f))

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = animationSpec),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = animationSpec),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showSeekBar) {
                ChapterNavigator(
                    isRtl = isRtl,
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onSliderValueChange = onSliderValueChange,
                )
            }
            BottomReaderBar(
                readingMode = readingMode,
                onClickReadingMode = onClickReadingMode,
                orientationMode = orientationMode,
                onClickOrientationMode = onClickOrientationMode,
                onClickSettings = onClickSettings,
            )
        }
    }
}
