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
import androidx.compose.runtime.remember
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.client.data.GalleryInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

object FavouriteStatusRouter {
    fun modifyFavourites(gid: Long, slot: Int) {
        _globalFlow.tryEmit(gid to slot)
    }

    private val listenerScope = CoroutineScope(Dispatchers.IO)

    private val _globalFlow = MutableSharedFlow<Pair<Long, Int>>(extraBufferCapacity = 1).apply {
        listenerScope.launch {
            collect { (gid, slot) ->
                EhDB.updateFavoriteSlot(gid, slot)
            }
        }
    }

    val globalFlow = _globalFlow.asSharedFlow()

    @Stable
    @Composable
    inline fun <R> collectAsState(initial: GalleryInfo, crossinline transform: @DisallowComposableCalls (Int) -> R) = remember {
        globalFlow.transform { (gid, slot) -> if (initial.gid == gid) emit(transform(slot)) }
    }.collectAsState(transform(initial.favoriteSlot))
}
