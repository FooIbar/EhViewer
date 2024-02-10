package com.hippo.ehviewer.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.big.History
import com.hippo.ehviewer.ui.LocalNavDrawerState
import com.hippo.ehviewer.ui.doGalleryInfoAction
import com.hippo.ehviewer.ui.main.GalleryInfoListItem
import com.hippo.ehviewer.ui.tools.Deferred
import com.hippo.ehviewer.ui.tools.FastScrollLazyColumn
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.SwipeToDismissBox2
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.util.FavouriteStatusRouter
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Destination
@Composable
fun HistoryScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val dialogState = LocalDialogState.current
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val historyData = rememberInVM {
        Pager(config = PagingConfig(pageSize = 20, jumpThreshold = 40)) {
            EhDB.historyLazyList
        }.flow.cachedIn(viewModelScope)
    }.collectAsLazyPagingItems()
    FavouriteStatusRouter.observe(historyData)
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.history)) },
                navigationIcon = {
                    val drawerState = LocalNavDrawerState.current
                    IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launchIO {
                            dialogState.awaitPermissionOrCancel(
                                confirmText = R.string.clear_all,
                                text = { Text(text = stringResource(id = R.string.clear_all_history)) },
                            )
                            EhDB.clearHistoryInfo()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.ClearAll, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        val marginH = dimensionResource(id = R.dimen.gallery_list_margin_h)
        val cardHeight by collectListThumbSizeAsState()
        FastScrollLazyColumn(
            modifier = Modifier.padding(horizontal = marginH).fillMaxSize(),
            contentPadding = paddingValues,
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
                                coroutineScope.launchIO {
                                    EhDB.deleteHistoryInfo(info)
                                }
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
                            onClick = {
                                navigator.navigate(info.asDst())
                            },
                            onLongClick = {
                                coroutineScope.launchIO {
                                    dialogState.doGalleryInfoAction(info, context)
                                }
                            },
                            info = info,
                            modifier = Modifier.height(cardHeight),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(cardHeight).fillMaxWidth())
                }
            }
        }
        Deferred({ delay(200) }) {
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
                    Text(
                        text = stringResource(id = R.string.no_history),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
        }
    }
}
