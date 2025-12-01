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
package com.hippo.ehviewer

import androidx.room.execSQL
import androidx.room.useWriterConnection
import arrow.fx.coroutines.resource
import arrow.fx.coroutines.resourceScope
import com.ehviewer.core.data.model.asEntity
import com.ehviewer.core.database.EhDatabase
import com.ehviewer.core.database.Schema17to18
import com.ehviewer.core.database.getDatabasePath
import com.ehviewer.core.database.model.DownloadArtist
import com.ehviewer.core.database.model.DownloadDirname
import com.ehviewer.core.database.model.DownloadInfo
import com.ehviewer.core.database.model.DownloadLabel
import com.ehviewer.core.database.model.Filter
import com.ehviewer.core.database.model.GalleryEntity
import com.ehviewer.core.database.model.HistoryInfo
import com.ehviewer.core.database.model.LocalFavoriteInfo
import com.ehviewer.core.database.model.ProgressInfo
import com.ehviewer.core.database.model.QuickSearch
import com.ehviewer.core.database.roomDb
import com.ehviewer.core.files.delete
import com.ehviewer.core.files.sendTo
import com.ehviewer.core.model.GalleryInfo
import com.hippo.ehviewer.download.DownloadManager
import kotlinx.coroutines.flow.Flow
import okio.Path

object EhDB {
    private const val DB_NAME = "eh.db"
    private val db = roomDb<EhDatabase>(DB_NAME) {
        addMigrations(Schema17to18())
    }

    suspend fun putGalleryInfo(galleryInfo: GalleryEntity) {
        db.galleryDao().upsert(galleryInfo)
    }

    private suspend fun deleteGalleryInfo(galleryInfo: GalleryEntity) {
        runCatching { db.galleryDao().delete(galleryInfo) }
    }

    suspend fun updateGalleryInfo(galleryInfoList: List<GalleryEntity>) {
        db.galleryDao().update(galleryInfoList)
    }

    fun getReadProgressFlow(gid: Long) = db.progressDao().getPageFlow(gid)
    suspend fun getReadProgress(gid: Long) = db.progressDao().getPage(gid)
    suspend fun putReadProgress(gid: Long, page: Int) = db.progressDao().upsert(ProgressInfo(gid, page))
    suspend fun clearProgressInfo() = db.progressDao().deleteAll()

    suspend fun getAllDownloadInfo() = db.downloadsDao().joinList().onEach {
        if (it.state == DownloadInfo.STATE_WAIT || it.state == DownloadInfo.STATE_DOWNLOAD) {
            it.state = DownloadInfo.STATE_NONE
        }
    }

    suspend fun updateDownloadInfo(downloadInfo: Collection<DownloadInfo>) {
        val dao = db.downloadsDao()
        dao.update(downloadInfo.map(DownloadInfo::downloadInfo))
    }

    suspend fun putDownloadInfo(downloadInfo: DownloadInfo) {
        putGalleryInfo(downloadInfo.galleryInfo)
        db.downloadsDao().upsert(downloadInfo.downloadInfo)
    }

    suspend fun removeDownloadInfo(downloadInfo: DownloadInfo) {
        val dao = db.downloadsDao()
        dao.delete(downloadInfo.downloadInfo)
        deleteGalleryInfo(downloadInfo.galleryInfo)
    }

    suspend fun randomLocalFav() = db.localFavoritesDao().random()

    suspend fun removeDownloadInfo(downloadInfo: List<DownloadInfo>) {
        val dao = db.downloadsDao()
        downloadInfo.forEach {
            dao.delete(it.downloadInfo)
            deleteGalleryInfo(it.galleryInfo)
        }
    }

    suspend fun getDownloadDirname(gid: Long): String? {
        val dao = db.downloadDirnameDao()
        val raw = dao.load(gid)
        return raw?.dirname
    }

    suspend fun putDownloadDirname(gid: Long, dirname: String) {
        val dao = db.downloadDirnameDao()
        dao.upsert(DownloadDirname(gid, dirname))
    }

    suspend fun removeDownloadDirname(gid: Long) {
        val dao = db.downloadDirnameDao()
        dao.deleteByKey(gid)
    }

    private suspend fun importDownloadDirname(downloadDirnameList: List<DownloadDirname>) {
        val dao = db.downloadDirnameDao()
        dao.insertOrIgnore(downloadDirnameList)
    }

    val downloadsCountByLabel
        get() = db.downloadsDao().countByLabel()

    val downloadsCountByArtist
        get() = db.downloadsDao().countByArtist()

    suspend fun getAllDownloadLabelList() = db.downloadLabelDao().list()

    suspend fun addDownloadLabel(raw: DownloadLabel): DownloadLabel {
        // Reset id
        raw.id = null
        val dao = db.downloadLabelDao()
        raw.id = dao.insert(raw)
        return raw
    }

    suspend fun updateDownloadLabel(raw: DownloadLabel) {
        val dao = db.downloadLabelDao()
        dao.update(raw)
    }

    suspend fun updateDownloadLabel(downloadLabels: List<DownloadLabel>) {
        val dao = db.downloadLabelDao()
        dao.update(downloadLabels)
    }

    suspend fun removeDownloadLabel(raw: DownloadLabel) {
        val dao = db.downloadLabelDao()
        dao.delete(raw)
        dao.fill(raw.position)
    }

    suspend fun searchDownloadLabel(keyword: String, limit: Int) = db.downloadLabelDao().search("%$keyword%", limit)

    suspend fun putDownloadArtist(gid: Long, artists: List<DownloadArtist>) {
        if (artists.isNotEmpty()) {
            val dao = db.downloadArtistDao()
            dao.deleteByGid(gid)
            dao.insertOrIgnore(artists)
        }
    }

    suspend fun removeLocalFavorites(galleryInfo: GalleryInfo) {
        db.localFavoritesDao().deleteByKey(galleryInfo.gid)
        deleteGalleryInfo(galleryInfo.asEntity())
    }

    suspend fun removeLocalFavorites(galleryInfoList: Collection<GalleryInfo>) {
        galleryInfoList.forEach {
            removeLocalFavorites(it)
        }
    }

    suspend fun containLocalFavorites(gid: Long): Boolean {
        val dao = db.localFavoritesDao()
        return dao.contains(gid)
    }

    suspend fun putLocalFavorites(galleryInfo: GalleryInfo) {
        putGalleryInfo(galleryInfo.asEntity())
        db.localFavoritesDao().upsert(LocalFavoriteInfo(galleryInfo.gid))
    }

    suspend fun putLocalFavorites(galleryInfoList: Collection<GalleryInfo>) {
        galleryInfoList.forEach {
            putLocalFavorites(it)
        }
    }

    private suspend fun importLocalFavorites(localFavorites: List<LocalFavoriteInfo>) {
        db.localFavoritesDao().insertOrIgnore(localFavorites)
    }

    suspend fun getAllQuickSearch() = db.quickSearchDao().list()

    suspend fun insertQuickSearch(quickSearch: QuickSearch) {
        val dao = db.quickSearchDao()
        quickSearch.id = dao.insert(quickSearch)
    }

    private suspend fun importQuickSearch(quickSearchList: List<QuickSearch>) {
        val dao = db.quickSearchDao()
        dao.insert(quickSearchList)
    }

    suspend fun deleteQuickSearch(quickSearch: QuickSearch) {
        val dao = db.quickSearchDao()
        dao.delete(quickSearch)
        dao.fill(quickSearch.position)
    }

    suspend fun updateQuickSearch(quickSearchList: List<QuickSearch>) {
        val dao = db.quickSearchDao()
        dao.update(quickSearchList)
    }

    val historyLazyList
        get() = db.historyDao().joinListLazy()

    fun searchHistory(keyword: String) = db.historyDao().joinListLazy("*$keyword*")

    val localFavLazyList
        get() = db.localFavoritesDao().joinListLazy()

    val localFavCount: Flow<Int>
        get() = db.localFavoritesDao().count()

    fun searchLocalFav(keyword: String) = db.localFavoritesDao().joinListLazy("*$keyword*")

    suspend fun putHistoryInfo(galleryInfo: GalleryInfo) {
        putGalleryInfo(galleryInfo.asEntity())
        db.historyDao().upsert(HistoryInfo(galleryInfo.gid))
    }

    suspend fun updateFavoriteSlot(gid: Long, slot: Int) {
        val dao = db.galleryDao()
        dao.load(gid)?.let {
            it.favoriteSlot = slot
            dao.update(it)
        }
    }

    private suspend fun importHistoryInfo(historyInfoList: List<HistoryInfo>) {
        val dao = db.historyDao()
        dao.insertOrIgnore(historyInfoList)
    }

    suspend fun deleteHistoryInfo(galleryInfo: GalleryEntity) {
        val dao = db.historyDao()
        dao.deleteByKey(galleryInfo.gid)
        deleteGalleryInfo(galleryInfo)
    }

    suspend fun clearHistoryInfo() {
        val dao = db.historyDao()
        val historyList = dao.list()
        dao.deleteAll()
        historyList.forEach { runCatching { db.galleryDao().deleteByKey(it.gid) } }
    }

    suspend fun getAllFilter() = db.filterDao().list()

    suspend fun addFilter(filter: Filter): Boolean {
        val existFilter = runCatching { db.filterDao().load(filter.text, filter.mode.field) }.getOrNull()
        return if (existFilter == null) {
            filter.id = null
            filter.id = db.filterDao().insert(filter)
            true
        } else {
            false
        }
    }

    suspend fun deleteFilter(filter: Filter) {
        db.filterDao().delete(filter)
    }

    suspend fun updateFilter(filter: Filter) {
        db.filterDao().update(filter)
    }

    suspend fun exportDB(file: Path) {
        db.useWriterConnection { conn ->
            conn.execSQL("PRAGMA wal_checkpoint(FULL)")
            conn.execSQL("VACUUM")
        }
        val dbFile = getDatabasePath(DB_NAME)
        dbFile sendTo file
    }

    suspend fun importDB(file: Path) = resourceScope {
        val tempDBName = "tmp.db"
        val dbFile = getDatabasePath(tempDBName)
        file sendTo dbFile
        val oldDB = resource {
            roomDb<EhDatabase>(tempDBName) {
                addMigrations(Schema17to18())
            }
        } release { db ->
            db.close()
            dbFile.delete()
        }

        db.galleryDao().insertOrIgnore(oldDB.galleryDao().list())
        db.progressDao().insertOrIgnore(oldDB.progressDao().list())

        val downloadLabelList = oldDB.downloadLabelDao().list()
        DownloadManager.addDownloadLabel(downloadLabelList)

        oldDB.downloadDirnameDao().list().let {
            importDownloadDirname(it)
        }

        val downloadInfoList = oldDB.downloadsDao().joinList().asReversed()
        DownloadManager.addDownload(downloadInfoList)

        val historyInfoList = oldDB.historyDao().list()
        importHistoryInfo(historyInfoList)

        val quickSearchList = oldDB.quickSearchDao().list()
        val currentQuickSearchList = db.quickSearchDao().list()
        val offset = currentQuickSearchList.size
        val importList = quickSearchList.filter { newQS ->
            currentQuickSearchList.none { it.name == newQS.name }
        }.onEachIndexed { index, q ->
            q.id = null
            q.position = index + offset
        }
        importQuickSearch(importList)

        oldDB.localFavoritesDao().list().let {
            importLocalFavorites(it)
        }

        oldDB.filterDao().list().forEach {
            addFilter(it)
        }
    }
}
