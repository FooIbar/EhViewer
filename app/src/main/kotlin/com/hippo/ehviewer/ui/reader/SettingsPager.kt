package com.hippo.ehviewer.ui.reader

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import kotlinx.coroutines.launch

private val tabs = arrayOf(
    R.string.pref_category_reading_mode,
    R.string.pref_category_general,
    R.string.custom_filter,
)

@Composable
fun SettingsPager(modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    PrimaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        containerColor = BottomSheetDefaults.ContainerColor,
    ) {
        tabs.forEachIndexed { index, res ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                text = { Text(text = stringResource(id = res)) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        verticalAlignment = Alignment.Top,
    ) { page ->
        when (page) {
            0 -> Spacer(modifier = Modifier.size(200.dp))
            1 -> ReaderGeneralSetting()
            2 -> ColorFilterSetting()
        }
    }
}
