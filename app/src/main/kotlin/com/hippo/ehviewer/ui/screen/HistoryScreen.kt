package com.hippo.ehviewer.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.big.History
import com.hippo.ehviewer.ui.composing
import com.hippo.ehviewer.ui.doGalleryInfoAction
import com.hippo.ehviewer.ui.main.GalleryInfoListItem
import com.hippo.ehviewer.ui.main.plus
import com.hippo.ehviewer.ui.tools.Deferred
import com.hippo.ehviewer.ui.tools.FastScrollLazyColumn
import com.hippo.ehviewer.ui.tools.SwipeToDismissBox2
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.util.FavouriteStatusRouter
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Destination
@Composable
fun HistoryScreen(navigator: DestinationsNavigator) = composing(navigator) {
    val title = stringResource(id = R.string.history)
    val hint = stringResource(R.string.search_bar_hint, title)

    var searchBarOffsetY by remember { mutableIntStateOf(0) }
    var keyword by rememberSaveable { mutableStateOf("") }

    val density = LocalDensity.current
    val historyData = rememberInVM {
        Pager(config = PagingConfig(pageSize = 20, jumpThreshold = 40)) {
            if (keyword.isNotEmpty()) {
                EhDB.searchHistory(keyword)
            } else {
                EhDB.historyLazyList
            }
        }.flow.cachedIn(viewModelScope)
    }.collectAsLazyPagingItems()
    FavouriteStatusRouter.Observe(historyData)
    SearchBarScreen(
        title = title,
        searchFieldHint = hint,
        onApplySearch = {
            keyword = it
            historyData.refresh()
        },
        onSearchExpanded = {},
        onSearchHidden = {},
        searchBarOffsetY = { searchBarOffsetY },
        trailingIcon = {
            IconButton(onClick = {
                launch {
                    awaitPermissionOrCancel(
                        confirmText = R.string.clear_all,
                        text = { Text(text = stringResource(id = R.string.clear_all_history)) },
                    )
                    EhDB.clearHistoryInfo()
                }
            }) {
                Icon(imageVector = Icons.Default.ClearAll, contentDescription = null)
            }
        },
    ) { paddingValues ->
        val searchBarConnection = remember {
            val topPaddingPx = with(density) { paddingValues.calculateTopPadding().roundToPx() }
            object : NestedScrollConnection {
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    val dy = -consumed.y
                    searchBarOffsetY = (searchBarOffsetY - dy).roundToInt().coerceIn(-topPaddingPx, 0)
                    return Offset.Zero // We never consume it
                }
            }
        }
        val marginH = dimensionResource(id = R.dimen.gallery_list_margin_h)
        val cardHeight by collectListThumbSizeAsState()
        val showPages by Settings.showGalleryPages.collectAsState()
        FastScrollLazyColumn(
            modifier = Modifier.nestedScroll(searchBarConnection).fillMaxSize(),
            contentPadding = paddingValues + PaddingValues(horizontal = marginH),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.gallery_list_interval)),
        ) {
            items(
                count = historyData.itemCount,
                key = historyData.itemKey(key = { item -> item.gid }),
                contentType = historyData.itemContentType(),
            ) { index ->
                val info = historyData[index]
                if (info != null) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                launch { EhDB.deleteHistoryInfo(info) }
                            }
                            true
                        },
                    )
                    SwipeToDismissBox2(
                        state = dismissState,
                        backgroundContent = {},
                        modifier = Modifier.animateItemPlacement(),
                    ) {
                        GalleryInfoListItem(
                            onClick = { navigate(info.asDst()) },
                            onLongClick = { launch { doGalleryInfoAction(info) } },
                            info = info,
                            showPages = showPages,
                            modifier = Modifier.height(cardHeight),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(cardHeight).fillMaxWidth())
                }
            }
        }
        Deferred(keyword, { delay(200) }) {
            if (historyData.itemCount == 0) {
                Column(
                    modifier = Modifier.padding(paddingValues).padding(horizontal = marginH).fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = EhIcons.Big.Default.History,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    val emptyHint = if (keyword.isEmpty()) {
                        stringResource(id = R.string.no_history)
                    } else {
                        stringResource(id = R.string.gallery_list_empty_hit)
                    }
                    Text(
                        text = emptyHint,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
        }
    }
}
