/*
 * Copyright 2019 Hippo Seven
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
package com.hippo.ehviewer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.paging.compose.LazyPagingItems
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.ui.tools.launchInVM
import com.hippo.ehviewer.ui.tools.rememberUpdatedStateInVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

object FavouriteStatusRouter {
    fun notify(galleryInfo: GalleryInfo) {
        globalFlow.tryEmit(galleryInfo)
    }

    private val listenerScope = CoroutineScope(Dispatchers.IO)

    val globalFlow = MutableSharedFlow<GalleryInfo>(extraBufferCapacity = 1).apply {
        listenerScope.launch {
            collect { info ->
                EhDB.updateFavoriteSlot(info.gid, info.favoriteSlot)
            }
        }
    }

    suspend fun collect(collector: FlowCollector<GalleryInfo>): Nothing = globalFlow.collect(collector)

    @Stable
    @Composable
    inline fun <R> collectAsState(initial: GalleryInfo, crossinline transform: @DisallowComposableCalls (Int) -> R) = remember {
        globalFlow.transform { info -> if (initial.gid == info.gid) emit(transform(info.favoriteSlot)) }
    }.collectAsState(transform(initial.favoriteSlot))

    @Composable
    fun Observe(list: LazyPagingItems<out GalleryInfo>) {
        val realList by rememberUpdatedStateInVM(newValue = list.itemSnapshotList.items)
        launchInVM {
            collect { info ->
                realList.forEach { item ->
                    if (item.gid == info.gid) item.favoriteSlot = info.favoriteSlot
                }
            }
        }
    }
}
