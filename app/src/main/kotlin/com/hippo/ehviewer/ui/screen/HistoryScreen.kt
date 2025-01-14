package com.hippo.ehviewer.ui.screen

import androidx.compose.animation.AnimatedVisibilityScope
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
import androidx.compose.material3.fork.SwipeToDismissBox
import androidx.compose.material3.fork.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.paging.map
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.big.History
import com.hippo.ehviewer.ui.DrawerHandle
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.ui.doGalleryInfoAction
import com.hippo.ehviewer.ui.main.GalleryInfoListItem
import com.hippo.ehviewer.ui.main.plus
import com.hippo.ehviewer.ui.tools.Await
import com.hippo.ehviewer.ui.tools.FastScrollLazyColumn
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.ui.tools.thenIf
import com.hippo.ehviewer.util.FavouriteStatusRouter
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.HistoryScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val title = stringResource(id = R.string.history)
    val hint = stringResource(R.string.search_bar_hint, title)
    val animateItems by Settings.animateItems.collectAsState()

    var searchBarExpanded by rememberSaveable { mutableStateOf(false) }
    var searchBarOffsetY by remember { mutableIntStateOf(0) }
    var keyword by rememberSaveable { mutableStateOf("") }

    DrawerHandle(!searchBarExpanded)

    val density = LocalDensity.current
    val historyData = rememberInVM {
        Pager(config = PagingConfig(pageSize = 20, jumpThreshold = 40)) {
            if (keyword.isNotEmpty()) {
                EhDB.searchHistory(keyword)
            } else {
                EhDB.historyLazyList
            }
        }.flow.map { data ->
            val favCat = Settings.favCat
            data.map {
                it.apply { favoriteName = favCat.getOrNull(favoriteSlot) }
            }
        }.cachedIn(viewModelScope)
    }.collectAsLazyPagingItems()
    FavouriteStatusRouter.Observe(historyData)
    SearchBarScreen(
        onApplySearch = {
            keyword = it
            historyData.refresh()
        },
        expanded = searchBarExpanded,
        onExpandedChange = { searchBarExpanded = it },
        title = title,
        searchFieldHint = hint,
        searchBarOffsetY = { searchBarOffsetY },
        trailingIcon = {
            IconButton(onClick = {
                launch {
                    awaitConfirmationOrCancel(
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
                    val dismissState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(dismissState) {
                        snapshotFlow { dismissState.currentValue }.collect {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                EhDB.deleteHistoryInfo(info)
                            }
                        }
                    }
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {},
                        modifier = Modifier.thenIf(animateItems) { animateItem() },
                        enableDismissFromStartToEnd = false,
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
        Await(keyword, { delay(200) }) {
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
