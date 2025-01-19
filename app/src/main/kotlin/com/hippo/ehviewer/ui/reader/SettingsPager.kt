package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ehviewer.core.common.Res
import com.ehviewer.core.common.custom_filter
import com.ehviewer.core.common.pref_category_general
import com.ehviewer.core.common.pref_category_reading_mode
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val tabs = arrayOf(
    Res.string.pref_category_reading_mode,
    Res.string.pref_category_general,
    Res.string.custom_filter,
)

@Composable
fun SettingsPager(modifier: Modifier = Modifier, onPageSelected: (Int) -> Unit) {
    val pagerState = rememberPagerState { tabs.size }
    LaunchedEffect(onPageSelected) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageSelected(page)
        }
    }
    val scope = rememberCoroutineScope()
    PrimaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        containerColor = BottomSheetDefaults.ContainerColor,
    ) {
        tabs.forEachIndexed { index, res ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                text = { Text(text = stringResource(res)) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        verticalAlignment = Alignment.Top,
    ) { page ->
        ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
            when (page) {
                0 -> ReaderModeSetting()
                1 -> ReaderGeneralSetting()
                2 -> ColorFilterSetting()
            }
        }
    }
}
