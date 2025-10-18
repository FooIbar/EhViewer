package eu.kanade.tachiyomi.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.ui.main.NavigationIcon
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

private val animationSpec = tween<IntOffset>(200)

@Composable
context(navigator: DestinationsNavigator)
fun BoxScope.ReaderAppBars(
    visible: Boolean,
    title: String,
    isRtl: Boolean,
    showSeekBar: Boolean,
    currentPage: Int,
    totalPages: Int,
    onSliderValueChange: (Int) -> Unit,
    onClickSettings: () -> Unit,
) {
    val backgroundColor = BottomAppBarDefaults.containerColor.copy(alpha = if (isSystemInDarkTheme()) 0.9f else 0.95f)

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility.only(WindowInsetsSides.Horizontal)).align(Alignment.TopStart),
        enter = slideInVertically(initialOffsetY = { -it }, animationSpec = animationSpec),
        exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = animationSpec),
    ) {
        TopAppBar(
            title = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor),
            navigationIcon = { NavigationIcon() },
        )
    }

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility.only(WindowInsetsSides.Horizontal)).align(Alignment.BottomStart),
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = animationSpec),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = animationSpec),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showSeekBar && totalPages > 1) {
                ChapterNavigator(
                    isRtl = isRtl,
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onSliderValueChange = onSliderValueChange,
                    containerColor = backgroundColor,
                )
            }
            BottomReaderBar(onClickSettings = onClickSettings, containerColor = backgroundColor)
        }
    }
}
