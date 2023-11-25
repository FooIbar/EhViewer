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
package com.hippo.ehviewer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.LOCAL_FAVORITED
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.download.downloadDir
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.util.FavouriteStatusRouter
import com.hippo.ehviewer.util.findActivity
import com.hippo.ehviewer.util.isAtLeastT
import com.hippo.ehviewer.util.mapToLongArray
import com.hippo.ehviewer.util.requestPermission
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.tarsin.coroutines.runSuspendCatching
import splitties.init.appCtx

private fun removeNoMediaFile(downloadDir: UniFile) {
    val noMedia = downloadDir.subFile(".nomedia") ?: return
    noMedia.delete()
}

private fun ensureNoMediaFile(downloadDir: UniFile) {
    downloadDir.createFile(".nomedia") ?: return
}

private val lck = Mutex()

suspend fun keepNoMediaFileStatus() {
    lck.withLock {
        if (Settings.mediaScan) {
            removeNoMediaFile(downloadLocation)
        } else {
            ensureNoMediaFile(downloadLocation)
        }
    }
}

fun getFavoriteIcon(favorited: Boolean) =
    if (favorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder

suspend fun DialogState.startDownload(
    context: Context,
    forceDefault: Boolean,
    vararg galleryInfos: BaseGalleryInfo,
) = with(context) {
    if (isAtLeastT) {
        requestPermission(Manifest.permission.POST_NOTIFICATIONS)
    }
    val (toStart, toAdd) = galleryInfos.partition { DownloadManager.containDownloadInfo(it.gid) }
    if (toStart.isNotEmpty()) {
        val intent = Intent(context, DownloadService::class.java)
        intent.action = DownloadService.ACTION_START_RANGE
        val list = toStart.mapToLongArray(GalleryInfo::gid)
        intent.putExtra(DownloadService.KEY_GID_LIST, list)
        ContextCompat.startForegroundService(context, intent)
    }
    if (toAdd.isEmpty()) {
        return with(findActivity<MainActivity>()) {
            showTip(R.string.added_to_download_list, BaseScene.LENGTH_SHORT)
        }
    }
    var justStart = forceDefault
    var label: String? = null
    // Get default download label
    if (!justStart && Settings.hasDefaultDownloadLabel) {
        label = Settings.defaultDownloadLabel
        justStart = label == null || DownloadManager.containLabel(label)
    }
    // If there is no other label, just use null label
    if (!justStart && DownloadManager.labelList.isEmpty()) {
        justStart = true
        label = null
    }
    if (justStart) {
        // Got default label
        for (gi in toAdd) {
            val intent = Intent(context, DownloadService::class.java)
            intent.action = DownloadService.ACTION_START
            intent.putExtra(DownloadService.KEY_LABEL, label)
            intent.putExtra(DownloadService.KEY_GALLERY_INFO, gi)
            ContextCompat.startForegroundService(context, intent)
        }
        // Notify
        with(findActivity<MainActivity>()) {
            showTip(R.string.added_to_download_list, BaseScene.LENGTH_SHORT)
        }
    } else {
        // Let use chose label
        val list = DownloadManager.labelList
        val items = arrayOf(
            getString(R.string.default_download_label_name),
            *list.map { it.label }.toTypedArray(),
        )
        val (selected, checked) = showSelectItemWithCheckBox(
            *items,
            title = R.string.download,
            checkBoxText = R.string.remember_download_label,
        )
        val label1 = if (selected == 0) null else items[selected].takeIf { DownloadManager.containLabel(it) }
        // Start download
        for (gi in toAdd) {
            val intent = Intent(context, DownloadService::class.java)
            intent.action = DownloadService.ACTION_START
            intent.putExtra(DownloadService.KEY_LABEL, label1)
            intent.putExtra(DownloadService.KEY_GALLERY_INFO, gi)
            ContextCompat.startForegroundService(context, intent)
        }
        // Save settings
        if (checked) {
            Settings.hasDefaultDownloadLabel = true
            Settings.defaultDownloadLabel = label1
        } else {
            Settings.hasDefaultDownloadLabel = false
        }
        with(context.findActivity<MainActivity>()) {
            showTip(R.string.added_to_download_list, BaseScene.LENGTH_SHORT)
        }
    }
}

suspend fun DialogState.modifyFavorites(galleryInfo: BaseGalleryInfo): Boolean {
    val localFavorited = EhDB.containLocalFavorites(galleryInfo.gid)
    if (EhCookieStore.hasSignedIn()) {
        val isFavorited = galleryInfo.favoriteSlot != NOT_FAVORITED
        val defaultFavSlot = Settings.defaultFavSlot
        if (defaultFavSlot == -2) {
            val localFav = getFavoriteIcon(localFavorited) to appCtx.getString(R.string.local_favorites)
            val cloudFav = Settings.favCat.mapIndexed { index, name ->
                getFavoriteIcon(galleryInfo.favoriteSlot == index) to name
            }.toTypedArray()
            val items = if (isFavorited) {
                val remove = Icons.Default.HeartBroken to appCtx.getString(R.string.remove_from_favourites)
                arrayOf(remove, localFav, *cloudFav)
            } else {
                arrayOf(localFav, *cloudFav)
            }
            val (slot, note) = showSelectItemWithIconAndTextField(
                *items,
                title = R.string.add_favorites_dialog_title,
                hint = R.string.favorite_note,
                maxChar = MAX_FAVNOTE_CHAR,
            )
            return doModifyFavorites(galleryInfo, if (isFavorited) slot - 2 else slot - 1, localFavorited, note)
        } else {
            return doModifyFavorites(galleryInfo, if (isFavorited) NOT_FAVORITED else defaultFavSlot, localFavorited)
        }
    } else {
        return doModifyFavorites(galleryInfo, LOCAL_FAVORITED, localFavorited)
    }
}

private suspend fun doModifyFavorites(
    galleryInfo: BaseGalleryInfo,
    slot: Int = NOT_FAVORITED,
    localFavorited: Boolean = true,
    note: String = "",
): Boolean {
    val add = when (slot) {
        NOT_FAVORITED -> { // Remove from cloud favorites first
            if (galleryInfo.favoriteSlot > LOCAL_FAVORITED) {
                EhEngine.modifyFavorites(galleryInfo.gid, galleryInfo.token)
            } else if (localFavorited) {
                EhDB.removeLocalFavorites(galleryInfo)
            }
            false
        }

        LOCAL_FAVORITED -> {
            if (localFavorited) {
                EhDB.removeLocalFavorites(galleryInfo)
            } else {
                EhDB.putLocalFavorites(galleryInfo)
            }
            !localFavorited
        }

        in 0..9 -> {
            EhEngine.modifyFavorites(galleryInfo.gid, galleryInfo.token, slot, note)
            true
        }

        else -> throw EhException("Invalid favorite slot!")
    }
    if (add) { // Cloud favorites have priority
        if (slot != LOCAL_FAVORITED || galleryInfo.favoriteSlot == NOT_FAVORITED) {
            galleryInfo.favoriteSlot = slot
            galleryInfo.favoriteName = Settings.favCat.getOrNull(slot)
            FavouriteStatusRouter.modifyFavourites(galleryInfo.gid, slot)
        }
    } else if (slot != LOCAL_FAVORITED || galleryInfo.favoriteSlot == LOCAL_FAVORITED) {
        val newSlot = if (galleryInfo.favoriteSlot > LOCAL_FAVORITED && localFavorited) LOCAL_FAVORITED else NOT_FAVORITED
        galleryInfo.favoriteSlot = newSlot
        galleryInfo.favoriteName = null
        FavouriteStatusRouter.modifyFavourites(galleryInfo.gid, newSlot)
    }
    return add
}

suspend fun removeFromFavorites(galleryInfo: BaseGalleryInfo) = doModifyFavorites(galleryInfo)

fun Context.navToReader(info: BaseGalleryInfo, page: Int = -1) {
    val intent = Intent(this, ReaderActivity::class.java)
    intent.action = ReaderActivity.ACTION_EH
    intent.putExtra(ReaderActivity.KEY_GALLERY_INFO, info)
    intent.putExtra(ReaderActivity.KEY_PAGE, page)
    startActivity(intent)
}

suspend fun DialogState.doGalleryInfoAction(info: BaseGalleryInfo, context: Context) {
    val downloaded = DownloadManager.getDownloadState(info.gid) != DownloadInfo.STATE_INVALID
    val favourite = info.favoriteSlot != NOT_FAVORITED
    val selected = if (!downloaded) {
        showSelectItemWithIcon(
            Icons.AutoMirrored.Default.MenuBook to R.string.read,
            Icons.Default.Download to R.string.download,
            if (!favourite) Icons.Default.Favorite to R.string.add_to_favourites else Icons.Default.HeartBroken to R.string.remove_from_favourites,
            title = EhUtils.getSuitableTitle(info),
        )
    } else {
        showSelectItemWithIcon(
            Icons.AutoMirrored.Default.MenuBook to R.string.read,
            Icons.Default.Delete to R.string.delete_downloads,
            if (!favourite) Icons.Default.Favorite to R.string.add_to_favourites else Icons.Default.HeartBroken to R.string.remove_from_favourites,
            Icons.AutoMirrored.Default.DriveFileMove to R.string.download_move_dialog_title,
            title = EhUtils.getSuitableTitle(info),
        )
    }
    with(context.findActivity<MainActivity>()) {
        when (selected) {
            0 -> navToReader(info)
            1 -> withUIContext {
                if (downloaded) {
                    confirmRemoveDownload(info)
                } else {
                    startDownload(context, false, info)
                }
            }

            2 -> if (favourite) {
                runSuspendCatching {
                    removeFromFavorites(info)
                    showTip(R.string.remove_from_favorite_success, BaseScene.LENGTH_SHORT)
                }.onFailure {
                    showTip(R.string.remove_from_favorite_failure, BaseScene.LENGTH_LONG)
                }
            } else {
                runSuspendCatching {
                    modifyFavorites(info)
                    showTip(R.string.add_to_favorite_success, BaseScene.LENGTH_SHORT)
                }.onFailure {
                    showTip(R.string.add_to_favorite_failure, BaseScene.LENGTH_LONG)
                }
            }

            3 -> showMoveDownloadLabel(info)
        }
        true
    }
}

private const val MAX_FAVNOTE_CHAR = 200

suspend fun DialogState.confirmRemoveDownload(info: GalleryInfo, onDismiss: () -> Unit = {}) {
    var checked by mutableStateOf(Settings.removeImageFiles)
    awaitPermissionOrCancel(
        title = R.string.download_remove_dialog_title,
        onDismiss = onDismiss,
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.download_remove_dialog_message, info.title.orEmpty()),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { checked = !checked },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                    Text(text = stringResource(id = R.string.download_remove_dialog_check_text))
                }
            }
        },
    )
    Settings.removeImageFiles = checked
    withIOContext {
        DownloadManager.deleteDownload(info.gid, checked)
    }
}

suspend fun DialogState.confirmRemoveDownloadRange(list: List<DownloadInfo>) {
    var checked by mutableStateOf(Settings.removeImageFiles)
    awaitPermissionOrCancel(
        title = R.string.download_remove_dialog_title,
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.download_remove_dialog_message_2, list.size),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { checked = !checked },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                    Text(text = stringResource(id = R.string.download_remove_dialog_check_text))
                }
            }
        },
    )
    Settings.removeImageFiles = checked
    withIOContext {
        // Delete
        DownloadManager.deleteRangeDownload(list.mapToLongArray(DownloadInfo::gid))
        // Delete image files
        if (checked) {
            list.forEach { info ->
                // Delete file
                info.downloadDir?.delete()
                // Remove download path
                EhDB.removeDownloadDirname(info.gid)
            }
        }
    }
}

suspend fun DialogState.showMoveDownloadLabel(info: GalleryInfo) {
    val defaultLabel = appCtx.getString(R.string.default_download_label_name)
    val labels = DownloadManager.labelList.map { it.label }.toTypedArray()
    val selected = showSelectItem(defaultLabel, *labels, title = R.string.download_move_dialog_title)
    val downloadInfo = DownloadManager.getDownloadInfo(info.gid) ?: return
    val label = if (selected == 0) null else labels[selected - 1]
    withUIContext { DownloadManager.changeLabel(listOf(downloadInfo), label) }
}
