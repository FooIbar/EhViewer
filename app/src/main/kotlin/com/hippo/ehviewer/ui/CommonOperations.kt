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
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
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
import com.hippo.ehviewer.download.tempDownloadDir
import com.hippo.ehviewer.ui.destinations.ReaderScreenDestination
import com.hippo.ehviewer.ui.reader.ReaderScreenArgs
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.LabeledCheckbox
import com.hippo.ehviewer.util.FavouriteStatusRouter
import com.hippo.ehviewer.util.bgWork
import com.hippo.ehviewer.util.findActivity
import com.hippo.ehviewer.util.isAtLeastT
import com.hippo.ehviewer.util.mapToLongArray
import com.hippo.ehviewer.util.requestPermission
import com.hippo.ehviewer.util.toEpochMillis
import com.hippo.ehviewer.util.toLocalDateTime
import com.hippo.files.delete
import com.hippo.files.isDirectory
import com.hippo.files.openOutputStream
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import moe.tarsin.coroutines.runSuspendCatching
import okio.Path
import splitties.init.appCtx

private fun removeNoMediaFile(downloadDir: Path) {
    (downloadDir / ".nomedia").delete()
}

private fun ensureNoMediaFile(downloadDir: Path) {
    (downloadDir / ".nomedia").openOutputStream().close()
}

private val lck = Mutex()

suspend fun keepNoMediaFileStatus(downloadDir: Path = downloadLocation) {
    if (downloadDir.isDirectory) {
        lck.withLock {
            if (Settings.mediaScan) {
                removeNoMediaFile(downloadDir)
            } else {
                ensureNoMediaFile(downloadDir)
            }
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
            showTip(R.string.added_to_download_list)
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
            showTip(R.string.added_to_download_list)
        }
    } else {
        // Let use chose label
        val list = DownloadManager.labelList
        val items = buildList {
            add(getString(R.string.default_download_label_name))
            list.forEach {
                add(it.label)
            }
        }
        val (selected, checked) = awaitSelectItemWithCheckBox(
            items,
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
            showTip(R.string.added_to_download_list)
        }
    }
}

suspend fun DialogState.modifyFavorites(galleryInfo: BaseGalleryInfo): Boolean {
    val localFavorited = EhDB.containLocalFavorites(galleryInfo.gid)
    if (Settings.hasSignedIn.value) {
        val isFavorited = galleryInfo.favoriteSlot != NOT_FAVORITED
        val defaultFavSlot = Settings.defaultFavSlot
        if (defaultFavSlot == -2) {
            val localFav = getFavoriteIcon(localFavorited) to appCtx.getString(R.string.local_favorites)
            val cloudFav = Settings.favCat.mapIndexed { index, name ->
                getFavoriteIcon(galleryInfo.favoriteSlot == index) to name
            }
            val items = buildList {
                if (isFavorited) {
                    val remove = Icons.Default.HeartBroken to appCtx.getString(R.string.remove_from_favourites)
                    add(remove)
                }
                add(localFav)
                addAll(cloudFav)
            }
            if (galleryInfo.favoriteSlot >= 0 && galleryInfo.favoriteNote == null) {
                galleryInfo.favoriteNote = bgWork { EhEngine.getFavoriteNote(galleryInfo.gid, galleryInfo.token) }
            }
            val (slot, note) = awaitSelectItemWithIconAndTextField(
                items,
                title = R.string.add_favorites_dialog_title,
                hint = R.string.favorite_note,
                initialNote = galleryInfo.favoriteNote.orEmpty(),
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
            galleryInfo.favoriteNote = note
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

suspend fun removeFromFavorites(galleryInfo: BaseGalleryInfo) = doModifyFavorites(
    galleryInfo = galleryInfo,
    localFavorited = EhDB.containLocalFavorites(galleryInfo.gid),
)

fun DestinationsNavigator.navToReader(info: BaseGalleryInfo, page: Int = -1) =
    navToReader(ReaderScreenArgs.Gallery(info, page))

fun DestinationsNavigator.navToReader(uri: Uri) =
    navToReader(ReaderScreenArgs.Archive(uri))

private fun DestinationsNavigator.navToReader(args: ReaderScreenArgs) =
    navigate(ReaderScreenDestination(args)) { launchSingleTop = true }

context(DialogState, Context, DestinationsNavigator)
suspend fun doGalleryInfoAction(info: BaseGalleryInfo) {
    val downloaded = DownloadManager.getDownloadState(info.gid) != DownloadInfo.STATE_INVALID
    val favorited = info.favoriteSlot != NOT_FAVORITED
    val items = buildList {
        add(Icons.AutoMirrored.Default.MenuBook to R.string.read)
        val download = if (downloaded) {
            Icons.Default.Delete to R.string.delete_downloads
        } else {
            Icons.Default.Download to R.string.download
        }
        add(download)
        val favorite = if (favorited) {
            Icons.Default.HeartBroken to R.string.remove_from_favourites
        } else {
            Icons.Default.Favorite to R.string.add_to_favourites
        }
        add(favorite)
        if (downloaded) {
            add(Icons.AutoMirrored.Default.DriveFileMove to R.string.download_move_dialog_title)
        }
    }
    val selected = awaitSelectItemWithIcon(items, EhUtils.getSuitableTitle(info))
    with(findActivity<MainActivity>()) {
        when (selected) {
            0 -> {
                EhDB.putHistoryInfo(info)
                navToReader(info)
            }

            1 -> withUIContext {
                if (downloaded) {
                    confirmRemoveDownload(info)
                } else {
                    startDownload(this@with, false, info)
                }
            }

            2 -> if (favorited) {
                runSuspendCatching {
                    removeFromFavorites(info)
                    showTip(R.string.remove_from_favorite_success)
                }.onFailure {
                    showTip(R.string.remove_from_favorite_failure)
                }
            } else {
                runSuspendCatching {
                    modifyFavorites(info)
                    showTip(R.string.add_to_favorite_success)
                }.onFailure {
                    showTip(R.string.add_to_favorite_failure)
                }
            }

            3 -> showMoveDownloadLabel(info)
        }
        true
    }
}

private const val MAX_FAVNOTE_CHAR = 200

private suspend fun DialogState.confirmRemoveDownload(text: String): Boolean {
    val checked = awaitResult(
        initial = Settings.removeImageFiles,
        title = R.string.download_remove_dialog_title,
    ) {
        Column {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            LabeledCheckbox(
                modifier = Modifier.fillMaxWidth(),
                checked = expectedValue,
                onCheckedChange = { expectedValue = it },
                label = stringResource(id = R.string.download_remove_dialog_check_text),
                indication = null,
            )
        }
    }
    Settings.removeImageFiles = checked
    return checked
}

suspend fun DialogState.confirmRemoveDownload(info: GalleryInfo) {
    val text = appCtx.getString(R.string.download_remove_dialog_message, EhUtils.getSuitableTitle(info))
    val checked = confirmRemoveDownload(text)
    withIOContext {
        DownloadManager.deleteDownload(info.gid, checked)
    }
}

suspend fun DialogState.confirmRemoveDownloadRange(list: Collection<DownloadInfo>) {
    val text = appCtx.getString(R.string.download_remove_dialog_message_2, list.size)
    val checked = confirmRemoveDownload(text)
    withIOContext {
        // Delete
        DownloadManager.deleteRangeDownload(list.mapToLongArray(DownloadInfo::gid))
        // Delete image files
        if (checked) {
            list.forEach { info ->
                // Delete file
                info.downloadDir?.delete()
                info.tempDownloadDir?.delete()
                // Remove download path
                EhDB.removeDownloadDirname(info.gid)
            }
        }
    }
}

suspend fun DialogState.showMoveDownloadLabel(info: GalleryInfo) {
    val defaultLabel = appCtx.getString(R.string.default_download_label_name)
    val labels = buildList {
        add(defaultLabel)
        DownloadManager.labelList.forEach {
            add(it.label)
        }
    }
    val selected = awaitSelectItem(labels, R.string.download_move_dialog_title)
    val downloadInfo = DownloadManager.getDownloadInfo(info.gid) ?: return
    val label = if (selected == 0) null else labels[selected]
    DownloadManager.changeLabel(listOf(downloadInfo), label)
}

suspend fun DialogState.showMoveDownloadLabelList(list: Collection<DownloadInfo>): String? {
    val defaultLabel = appCtx.getString(R.string.default_download_label_name)
    val labels = buildList {
        add(defaultLabel)
        DownloadManager.labelList.forEach {
            add(it.label)
        }
    }
    val selected = awaitSelectItem(labels, R.string.download_move_dialog_title)
    val label = if (selected == 0) null else labels[selected]
    DownloadManager.changeLabel(list, label)
    return label
}

suspend fun DialogState.awaitSelectDate(): String? {
    val initial = LocalDate(2007, 3, 21)
    val yesterday = Clock.System.todayIn(TimeZone.UTC).minus(1, DateTimeUnit.DAY)
    val initialMillis = initial.toEpochMillis()
    val yesterdayMillis = yesterday.toEpochMillis()
    val dateRange = initialMillis..yesterdayMillis
    val dateMillis = awaitSelectDate(
        title = R.string.go_to,
        yearRange = initial.year..yesterday.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis in dateRange
        },
    )
    val date = dateMillis?.run { toLocalDateTime().date.toString() }
    return date
}
