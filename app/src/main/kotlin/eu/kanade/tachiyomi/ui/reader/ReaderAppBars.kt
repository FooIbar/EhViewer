package eu.kanade.tachiyomi.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

private val animationSpec = tween<IntOffset>(200)

@Composable
fun ReaderAppBars(
    visible: Boolean,
    isRtl: Boolean,
    showSeekBar: Boolean,
    currentPage: Int,
    totalPages: Int,
    onSliderValueChange: (Int) -> Unit,
    onClickSettings: () -> Unit,
    modifier: Modifier = Modifier,
) = AnimatedVisibility(
    visible = visible,
    modifier = modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    enter = slideInVertically(initialOffsetY = { it }, animationSpec = animationSpec),
    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = animationSpec),
) {
    val toolbarColor = BottomAppBarDefaults.containerColor.copy(alpha = if (isSystemInDarkTheme()) 0.9f else 0.95f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showSeekBar && totalPages > 1) {
            ChapterNavigator(
                isRtl = isRtl,
                currentPage = currentPage,
                totalPages = totalPages,
                onSliderValueChange = onSliderValueChange,
                containerColor = toolbarColor,
            )
        }
        BottomReaderBar(onClickSettings = onClickSettings, containerColor = toolbarColor)
    }
}
