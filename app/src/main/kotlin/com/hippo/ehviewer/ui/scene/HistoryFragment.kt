/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.Settings.listThumbSize
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.big.History
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.doGalleryInfoAction
import com.hippo.ehviewer.ui.main.GalleryInfoListItem
import com.hippo.ehviewer.ui.tools.CrystalCard
import com.hippo.ehviewer.ui.tools.Deferred
import com.hippo.ehviewer.ui.tools.FastScrollLazyColumn
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.LocalTouchSlopProvider
import com.hippo.ehviewer.ui.tools.rememberInVM
import com.hippo.ehviewer.util.findActivity
import com.ramcosta.composedestinations.annotation.Destination
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.pxToDp
import kotlinx.coroutines.delay

@Destination
@Composable
fun HistoryScreen(navigator: NavController) {
    val context = LocalContext.current
    val dialogState = LocalDialogState.current
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val historyData = rememberInVM {
        Pager(config = PagingConfig(pageSize = 20, jumpThreshold = 40)) {
            EhDB.historyLazyList
        }.flow.cachedIn(viewModelScope)
    }.collectAsLazyPagingItems()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.history)) },
                navigationIcon = {
                    IconButton(onClick = { context.findActivity<MainActivity>().openDrawer() }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launchIO {
                            dialogState.awaitPermissionOrCancel(
                                confirmText = R.string.clear_all,
                                dismissText = android.R.string.cancel,
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
        val cardHeight = remember { (3 * listThumbSize * 3).pxToDp.dp }
        FastScrollLazyColumn(
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.gallery_list_margin_h)),
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
                    val dismissState = rememberDismissState(
                        confirmValueChange = {
                            if (it == DismissValue.DismissedToStart) {
                                coroutineScope.launchIO {
                                    EhDB.deleteHistoryInfo(info)
                                }
                            }
                            true
                        },
                    )
                    LocalTouchSlopProvider(Settings.touchSlopFactor.toFloat()) {
                        SwipeToDismiss(
                            state = dismissState,
                            background = {},
                            dismissContent = {
                                // TODO: item delete & add animation
                                // Bug tracker: https://issuetracker.google.com/issues/150812265
                                GalleryInfoListItem(
                                    onClick = {
                                        navigator.navAnimated(
                                            R.id.galleryDetailScene,
                                            bundleOf(
                                                GalleryDetailScene.KEY_ACTION to GalleryDetailScene.ACTION_GALLERY_INFO,
                                                GalleryDetailScene.KEY_GALLERY_INFO to info,
                                            ),
                                        )
                                    },
                                    onLongClick = {
                                        coroutineScope.launchIO {
                                            dialogState.doGalleryInfoAction(info, context)
                                        }
                                    },
                                    info = info,
                                    modifier = Modifier.height(cardHeight),
                                )
                            },
                            directions = setOf(DismissDirection.EndToStart),
                        )
                    }
                } else {
                    CrystalCard(modifier = Modifier.height(cardHeight).fillMaxWidth()) {}
                }
            }
        }
        Deferred({ delay(200) }) {
            if (historyData.itemCount == 0) {
                Column(
                    modifier = Modifier.padding(paddingValues).fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        imageVector = EhIcons.Big.Default.History,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
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

class HistoryFragment : BaseScene() {
    // Disabled for breaking swipe-to-dismiss
    override val enableDrawerGestures = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeWithMD3 {
            val navController = remember { findNavController() }
            HistoryScreen(navController)
        }
    }
}
