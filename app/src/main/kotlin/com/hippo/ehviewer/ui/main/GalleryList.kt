package com.hippo.ehviewer.ui.main

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.exception.CloudflareBypassException
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.big.SadAndroid
import com.hippo.ehviewer.ui.WebViewActivity
import com.hippo.ehviewer.ui.screen.collectDetailSizeAsState
import com.hippo.ehviewer.ui.tools.FastScrollLazyVerticalGrid
import com.hippo.ehviewer.ui.tools.FastScrollLazyVerticalStaggeredGrid
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.util.ExceptionUtils
import kotlinx.coroutines.CoroutineScope

@Composable
fun GalleryList(
    modifier: Modifier = Modifier,
    data: LazyPagingItems<BaseGalleryInfo>,
    contentModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listMode: Int,
    detailListState: LazyGridState = rememberLazyGridState(),
    detailItemContent: @Composable (LazyGridItemScope.(BaseGalleryInfo) -> Unit),
    thumbListState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    thumbItemContent: @Composable (LazyStaggeredGridItemScope.(BaseGalleryInfo) -> Unit),
    refreshState: PullToRefreshState,
    onRefresh: suspend CoroutineScope.() -> Unit,
    onLoading: suspend CoroutineScope.() -> Unit,
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val marginH = dimensionResource(id = R.dimen.gallery_list_margin_h)
    val marginV = dimensionResource(id = R.dimen.gallery_list_margin_v)
    val realPadding = PaddingValues(
        top = contentPadding.calculateTopPadding() + marginV,
        bottom = contentPadding.calculateBottomPadding() + marginV,
        start = contentPadding.calculateStartPadding(layoutDirection) + marginH,
        end = contentPadding.calculateEndPadding(layoutDirection) + marginH,
    )
    val combinedModifier = contentModifier.nestedScroll(refreshState.nestedScrollConnection)
    Box(modifier = modifier.fillMaxSize()) {
        if (listMode == 0) {
            val columnWidth by collectDetailSizeAsState()
            FastScrollLazyVerticalGrid(
                columns = GridCells.Adaptive(columnWidth),
                modifier = combinedModifier,
                state = detailListState,
                contentPadding = realPadding,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.gallery_list_interval)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.gallery_list_interval)),
            ) {
                items(
                    count = data.itemCount,
                    key = data.itemKey(key = { item -> item.gid }),
                    contentType = data.itemContentType(),
                ) { index ->
                    val info = data[index]
                    if (info != null) {
                        detailItemContent(info)
                    }
                }
                if (data.loadState.append !is LoadState.NotLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LoadStateIndicator(state = data.loadState.append) {
                            data.retry()
                        }
                    }
                }
            }
        } else {
            val gridInterval = dimensionResource(R.dimen.gallery_grid_interval)
            val thumbColumns by Settings.thumbColumns.collectAsState()
            FastScrollLazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(thumbColumns),
                modifier = combinedModifier,
                state = thumbListState,
                verticalItemSpacing = gridInterval,
                horizontalArrangement = Arrangement.spacedBy(gridInterval),
                contentPadding = realPadding,
            ) {
                items(
                    count = data.itemCount,
                    key = data.itemKey(key = { item -> item.gid }),
                    contentType = data.itemContentType(),
                ) { index ->
                    val info = data[index]
                    if (info != null) {
                        thumbItemContent(info)
                    }
                }
                if (data.loadState.append !is LoadState.NotLoading) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        LoadStateIndicator(state = data.loadState.append) {
                            data.retry()
                        }
                    }
                }
            }
        }

        var isLoading by remember { mutableStateOf(false) }
        if (refreshState.isRefreshing) {
            LaunchedEffect(Unit) {
                if (data.loadState.prepend.endOfPaginationReached) {
                    onRefresh()
                } else {
                    data.retry()
                }
            }
            LaunchedEffect(data.loadState) {
                if (data.loadState.prepend is LoadState.Loading ||
                    data.loadState.refresh is LoadState.Loading
                ) {
                    isLoading = true
                } else {
                    if (isLoading) {
                        refreshState.endRefresh()
                        isLoading = false
                    }
                }
            }
        }

        val dialogState = LocalDialogState.current
        when (val state = data.loadState.refresh) {
            is LoadState.Loading -> if (!refreshState.isRefreshing) {
                LaunchedEffect(Unit) {
                    onLoading()
                }
                LaunchedEffect(Unit) {
                    if (listMode == 0) {
                        detailListState.scrollToItem(0)
                    } else {
                        thumbListState.scrollToItem(0)
                    }
                }
                Surface {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is LoadState.Error -> {
                Surface {
                    LaunchedEffect(state) {
                        if (state.error.cause is CloudflareBypassException) {
                            dialogState.awaitPermissionOrCancel(title = R.string.cloudflare_bypass_failed) {
                                Text(text = stringResource(id = R.string.open_in_webview))
                            }
                            context.startActivity(
                                Intent(context, WebViewActivity::class.java).apply {
                                    putExtra(WebViewActivity.KEY_URL, EhUrl.host)
                                },
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.widthIn(max = 228.dp)
                                .clip(ShapeDefaults.Small).clickable { data.retry() },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = EhIcons.Big.Default.SadAndroid,
                                contentDescription = null,
                                modifier = Modifier.padding(16.dp).size(120.dp),
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                            Text(
                                text = ExceptionUtils.getReadableString(state.error),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            is LoadState.NotLoading -> Unit
        }
    }
}
