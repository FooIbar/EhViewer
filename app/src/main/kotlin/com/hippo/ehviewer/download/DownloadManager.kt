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
package com.hippo.ehviewer.download

import android.net.Uri
import android.util.SparseLongArray
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.util.lerp
import arrow.fx.coroutines.parMapNotNull
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.DownloadLabel
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.spider.COMIC_INFO_FILE
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.SPIDER_INFO_FILENAME
import com.hippo.ehviewer.spider.SpiderQueen.OnSpiderListener
import com.hippo.ehviewer.spider.putToDownloadDir
import com.hippo.ehviewer.spider.readComicInfo
import com.hippo.ehviewer.spider.readCompatFromUniFile
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.insertWith
import com.hippo.ehviewer.util.mapNotNull
import com.hippo.ehviewer.util.runAssertingNotMainThread
import com.hippo.unifile.UniFile
import com.hippo.unifile.asUniFile
import com.hippo.unifile.asUniFileOrNull
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import splitties.preferences.edit

object DownloadManager : OnSpiderListener, CoroutineScope {
    override val coroutineContext = Dispatchers.IO + Job()

    // All download info list
    private val allInfoList = runAssertingNotMainThread {
        (EhDB.getAllDownloadInfo() as MutableList).apply { sortWith(comparator()) }
    }

    val downloadInfoList: List<DownloadInfo>
        get() = allInfoList

    // All download info map
    private val mAllInfoMap = allInfoList.associateBy { it.gid } as MutableMap<Long, DownloadInfo>

    // All labels without default label
    val labelList = runAssertingNotMainThread { EhDB.getAllDownloadLabelList() }.toMutableStateList()

    // Store download info wait to start
    private val mWaitList = ArrayDeque<DownloadInfo>()
    private var mDownloadListener: DownloadListener? = null
    private var mCurrentTask: DownloadInfo? = null
    private var mCurrentSpider: SpiderQueen? = null

    private val mutableNotifyFlow = MutableSharedFlow<DownloadInfo>(extraBufferCapacity = 1)
    val notifyFlow = mutableNotifyFlow.asSharedFlow()

    private val mutex = Mutex()

    fun containLabel(label: String?): Boolean {
        if (label == null) {
            return false
        }
        for ((label1) in labelList) {
            if (label == label1) {
                return true
            }
        }
        return false
    }

    fun containDownloadInfo(gid: Long) = mAllInfoMap.containsKey(gid)

    fun getDownloadInfo(gid: Long) = mAllInfoMap[gid]

    fun getDownloadState(gid: Long): Int {
        val info = mAllInfoMap[gid]
        return info?.state ?: DownloadInfo.STATE_INVALID
    }

    fun setDownloadListener(listener: DownloadListener?) {
        mDownloadListener = listener
    }

    private suspend fun ensureDownload() {
        mutex.withLock {
            if (mCurrentTask != null) {
                // Only one download
                return
            }

            // Get download from wait list
            if (!mWaitList.isEmpty()) {
                val info = mWaitList.removeFirst()
                mCurrentTask = info
                with(info) {
                    archiveFile?.let {
                        return onFinish(pages, pages, pages)
                    }
                    state = DownloadInfo.STATE_DOWNLOAD
                    speed = -1
                    remaining = -1
                    total = -1
                    finished = 0
                    downloaded = 0
                    legacy = -1
                }
                val spider = SpiderQueen.obtainSpiderQueen(info, SpiderQueen.MODE_DOWNLOAD)
                mCurrentSpider = spider
                spider.addOnSpiderListener(this)
                // Update in DB
                EhDB.putDownloadInfo(info)
                // Start speed count
                mSpeedReminder.start()
                // Notify start downloading
                mDownloadListener?.onStart(info)
                // Notify state update
                mutableNotifyFlow.emit(info)
            }
        }
    }

    suspend fun startDownload(galleryInfo: BaseGalleryInfo, label: String?) {
        if (mCurrentTask?.gid == galleryInfo.gid) {
            // It is current task
            return
        }

        // Check in download list
        var info = mAllInfoMap[galleryInfo.gid]
        if (info != null) { // Get it in download list
            if (info.state != DownloadInfo.STATE_WAIT) {
                // Set state DownloadInfo.STATE_WAIT
                info.state = DownloadInfo.STATE_WAIT
                // Add to wait list
                mWaitList.add(info)
                // Update in DB
                EhDB.putDownloadInfo(info)
                // Notify state update
                mutableNotifyFlow.emit(info)
                // Make sure download is running
                ensureDownload()
            }
        } else {
            // It is new download info
            info = DownloadInfo(galleryInfo, galleryInfo.putToDownloadDir())
            info.label = label
            info.state = DownloadInfo.STATE_WAIT

            // Add to all download list and map
            allInfoList.insertWith(info, comparator())
            mAllInfoMap[galleryInfo.gid] = info

            // Add to wait list
            mWaitList.add(info)

            // Save to
            EhDB.putDownloadInfo(info)

            // Notify
            mutableNotifyFlow.emit(info)
            // Make sure download is running
            ensureDownload()

            // Add it to history
            EhDB.putHistoryInfo(info.galleryInfo)
        }
    }

    suspend fun startRangeDownload(gidList: LongArray) {
        val updateList = gidList.mapNotNull { mAllInfoMap[it] }
            .filter {
                when (it.state) {
                    DownloadInfo.STATE_NONE, DownloadInfo.STATE_FAILED, DownloadInfo.STATE_FINISH -> true
                    else -> false
                }
            }
        if (updateList.isNotEmpty()) {
            updateList.onEach {
                it.state = DownloadInfo.STATE_WAIT
                EhDB.putDownloadInfo(it)
                mutableNotifyFlow.emit(it)
            }
            mWaitList.addAll(updateList)
            ensureDownload()
        }
    }

    @Stable
    @Composable
    fun collectDownloadState(gid: Long): State<Int> = remember {
        notifyFlow.transform { if (it.gid == gid) emit(getDownloadState(gid)) }
    }.collectAsState(getDownloadState(gid))

    @Stable
    @Composable
    fun collectContainDownloadInfo(gid: Long): State<Boolean> = remember {
        notifyFlow.transform { if (it.gid == gid) emit(containDownloadInfo(gid)) }
    }.collectAsState(containDownloadInfo(gid))

    @Stable
    @Composable
    inline fun <T> updatedDownloadInfo(info: DownloadInfo, crossinline transform: @DisallowComposableCalls DownloadInfo.() -> T): T = remember {
        notifyFlow.transform { if (it.gid == info.gid) emit(transform(it)) }
    }.collectAsState(transform(info)).value

    suspend fun startAllDownload() {
        val updateList = allInfoList.filter {
            when (it.state) {
                DownloadInfo.STATE_NONE, DownloadInfo.STATE_FAILED -> true
                else -> false
            }
        }
        if (updateList.isNotEmpty()) {
            updateList.onEach {
                it.state = DownloadInfo.STATE_WAIT
                EhDB.putDownloadInfo(it)
                mutableNotifyFlow.emit(it)
            }
            mWaitList.addAll(updateList)
            ensureDownload()
        }
    }

    suspend fun addDownload(downloadInfoList: List<DownloadInfo>) {
        val comparator = comparator()
        downloadInfoList.forEach { info ->
            if (containDownloadInfo(info.gid)) return@forEach

            // Ensure download state
            if (DownloadInfo.STATE_WAIT == info.state || DownloadInfo.STATE_DOWNLOAD == info.state) {
                info.state = DownloadInfo.STATE_NONE
            }

            // Add to all download list and map
            allInfoList.insertWith(info, comparator)
            mAllInfoMap[info.gid] = info

            // Save to
            EhDB.putDownloadInfo(info)
        }
    }

    suspend fun addDownloadLabel(downloadLabelList: List<DownloadLabel>) {
        val offset = downloadLabelList.size
        downloadLabelList.forEachIndexed { index, label ->
            if (!containLabel(label.label)) {
                label.position = index + offset
                labelList.add(EhDB.addDownloadLabel(label))
            }
        }
    }

    suspend fun restoreDownload(galleryInfo: BaseGalleryInfo, dirname: String) {
        val info = DownloadInfo(galleryInfo, dirname)
        info.state = DownloadInfo.STATE_NONE

        // Add to all download list and map
        allInfoList.insertWith(info, comparator())
        mAllInfoMap[galleryInfo.gid] = info

        // Save to
        EhDB.putDownloadInfo(info)

        // Notify
        mutableNotifyFlow.emit(info)
    }

    suspend fun stopDownload(gid: Long) {
        val info = stopDownloadInternal(gid)
        if (info != null) {
            // Update listener
            mutableNotifyFlow.emit(info)
            // Ensure download
            ensureDownload()
        }
    }

    suspend fun stopCurrentDownload() {
        val info = stopCurrentDownloadInternal()
        if (info != null) {
            // Update listener
            mutableNotifyFlow.emit(info)
            // Ensure download
            ensureDownload()
        }
    }

    suspend fun stopRangeDownload(gidList: LongArray) {
        stopRangeDownloadInternal(gidList)

        // Update listener
        gidList.mapNotNull { mAllInfoMap[it] }.forEach {
            mutableNotifyFlow.emit(it)
        }

        // Ensure download
        ensureDownload()
    }

    suspend fun stopAllDownload() {
        mutex.withLock {
            // Stop all in wait list
            for (info in mWaitList) {
                info.state = DownloadInfo.STATE_NONE
                // Update in DB
                EhDB.putDownloadInfo(info)

                // Notify
                mutableNotifyFlow.emit(info)
            }
            mWaitList.clear()

            // Stop current
            val info = stopCurrentDownloadInternal()
            info?.let { mutableNotifyFlow.emit(it) }
        }
    }

    suspend fun deleteDownload(gid: Long, deleteFiles: Boolean = false) {
        stopDownloadInternal(gid)
        val info = mAllInfoMap[gid]
        if (info != null) {
            // Remove from DB
            EhDB.removeDownloadInfo(info)

            // Remove all list and map
            allInfoList.remove(info)
            mAllInfoMap.remove(info.gid)

            // Update listener
            mutableNotifyFlow.emit(info)

            // Ensure download
            ensureDownload()

            if (deleteFiles) {
                info.downloadDir?.delete()
                EhDB.removeDownloadDirname(info.gid)
            }
        }
    }

    suspend fun deleteRangeDownload(gidList: LongArray) {
        stopRangeDownloadInternal(gidList)
        val list = gidList.mapNotNull { gid ->
            mAllInfoMap.remove(gid)
        }
        EhDB.removeDownloadInfo(list)
        allInfoList.removeAll(list.toSet())

        // Update listener
        list.forEach { mutableNotifyFlow.emit(it) }

        // Ensure download
        ensureDownload()
    }

    fun sortDownloads(mode: SortMode) {
        allInfoList.sortWith(mode.comparator())
    }

    suspend fun resetAllReadingProgress() = runCatching {
        EhDB.clearProgressInfo()
    }.onFailure { logcat(it) }

    // Update in DB
    // Update listener
    // No ensureDownload
    private suspend fun stopDownloadInternal(gid: Long): DownloadInfo? {
        // Check current task
        if (mCurrentTask?.gid == gid) {
            // Stop current
            return stopCurrentDownloadInternal()
        }
        val iterator = mWaitList.iterator()
        while (iterator.hasNext()) {
            val info = iterator.next()
            if (info.gid == gid) {
                // Remove from wait list
                iterator.remove()
                // Update state
                info.state = DownloadInfo.STATE_NONE
                // Update in DB
                EhDB.putDownloadInfo(info)
                return info
            }
        }
        return null
    }

    // Update in DB
    // Update mDownloadListener
    private suspend fun stopCurrentDownloadInternal(): DownloadInfo? {
        val info = mCurrentTask
        val spider = mCurrentSpider
        // Release spider
        if (spider != null) {
            spider.removeOnSpiderListener(this@DownloadManager)
            SpiderQueen.releaseSpiderQueen(spider, SpiderQueen.MODE_DOWNLOAD)
        }
        mCurrentTask = null
        mCurrentSpider = null
        // Stop speed reminder
        mSpeedReminder.stop()
        if (info == null) {
            return null
        }

        // Update state
        info.state = DownloadInfo.STATE_NONE
        // Update in DB
        EhDB.putDownloadInfo(info)
        // Listener
        mDownloadListener?.onCancel(info)
        return info
    }

    // Update in DB
    // Update mDownloadListener
    private suspend fun stopRangeDownloadInternal(gidList: LongArray) {
        // Two way
        if (gidList.size < mWaitList.size) {
            for (element in gidList) {
                stopDownloadInternal(element)
            }
        } else {
            // Check current task
            if (mCurrentTask != null && gidList.contains(mCurrentTask!!.gid)) {
                // Stop current
                stopCurrentDownloadInternal()
            }

            // Check all in wait list
            val iterator = mWaitList.iterator()
            while (iterator.hasNext()) {
                val info = iterator.next()
                if (gidList.contains(info.gid)) {
                    // Remove from wait list
                    iterator.remove()
                    // Update state
                    info.state = DownloadInfo.STATE_NONE
                    // Update in DB
                    EhDB.putDownloadInfo(info)
                }
            }
        }
    }

    /**
     * @param label Not allow new label
     */
    suspend fun changeLabel(list: Collection<DownloadInfo>, label: String?) {
        if (null != label && !containLabel(label)) {
            logcat(TAG, LogPriority.ERROR) { "Not exits label: $label" }
            return
        }
        list.forEach {
            it.label = label
        }
        EhDB.updateDownloadInfo(list)
    }

    suspend fun addLabel(label: String?) {
        if (label == null || containLabel(label)) {
            return
        }
        labelList.add(EhDB.addDownloadLabel(DownloadLabel(label, labelList.size)))
    }

    suspend fun renameLabel(from: String, to: String) {
        val index = labelList.indexOfFirst { it.label == from }
        if (index != -1) {
            val exist = labelList.removeAt(index)
            val new = exist.copy(label = to)
            labelList.add(index, new)
            EhDB.updateDownloadLabel(new)
            allInfoList.forEach {
                if (it.label == from) {
                    it.label = to
                }
            }
        }
    }

    suspend fun deleteLabel(label: String) {
        labelList.run {
            val index = indexOfFirst { it.label == label }
            subList(index + 1, size).forEach {
                it.position--
            }
            EhDB.removeDownloadLabel(removeAt(index))
        }
        allInfoList.forEach {
            if (it.label == label) {
                it.label = null
            }
        }
    }

    suspend fun readPagesFromLocal() {
        val list = allInfoList.filter { it.pages == 0 }.parMapNotNull(concurrency = 5) { info ->
            info.downloadDir?.run {
                val pages = findFile(SPIDER_INFO_FILENAME)?.let {
                    readCompatFromUniFile(it)?.pages
                } ?: findFile(COMIC_INFO_FILE)?.let {
                    readComicInfo(it)?.pageCount
                }
                pages?.let {
                    info.pages = it
                    info.galleryInfo
                }
            }
        }
        EhDB.updateGalleryInfo(list)
    }

    val isIdle: Boolean
        get() = mCurrentTask == null && mWaitList.isEmpty()

    override fun onGetPages(pages: Int) {
        launch {
            mCurrentTask?.let { info ->
                info.total = pages
                mutableNotifyFlow.emit(info)
            } ?: logcat(TAG, LogPriority.ERROR) { "Current task is null, but it should not be" }
        }
    }

    override fun onGet509(index: Int) {
        launch {
            mDownloadListener?.onGet509()
        }
    }

    override fun onPageDownload(
        index: Int,
        contentLength: Long,
        receivedSize: Long,
        bytesRead: Int,
    ) {
        launch {
            mSpeedReminder.onDownload(index, contentLength, receivedSize, bytesRead)
        }
    }

    override fun onPageSuccess(index: Int, finished: Int, downloaded: Int, total: Int) {
        launch {
            mSpeedReminder.onDone(index)
            mCurrentTask?.let { info ->
                info.finished = finished
                info.downloaded = downloaded
                info.total = total
                mDownloadListener?.onGetPage(info)
                mutableNotifyFlow.emit(info)
            } ?: logcat(TAG, LogPriority.ERROR) { "Current task is null, but it should not be" }
        }
    }

    override fun onPageFailure(
        index: Int,
        error: String?,
        finished: Int,
        downloaded: Int,
        total: Int,
    ) {
        launch {
            mSpeedReminder.onDone(index)
            mCurrentTask?.let { info ->
                info.finished = finished
                info.downloaded = downloaded
                info.total = total
                mutableNotifyFlow.emit(info)
            } ?: logcat(TAG, LogPriority.ERROR) { "Current task is null, but it should not be" }
        }
    }

    override fun onFinish(finished: Int, downloaded: Int, total: Int) {
        launch {
            mSpeedReminder.onFinish()
            mCurrentSpider?.let { spider ->
                mCurrentSpider = null
                spider.removeOnSpiderListener(DownloadManager)
                SpiderQueen.releaseSpiderQueen(spider, SpiderQueen.MODE_DOWNLOAD)
            }
            mCurrentTask?.let { info ->
                mCurrentTask = null
                mSpeedReminder.stop()
                info.finished = finished
                info.downloaded = downloaded
                info.total = total
                info.legacy = total - finished
                if (info.legacy == 0) {
                    info.state = DownloadInfo.STATE_FINISH
                } else {
                    info.state = DownloadInfo.STATE_FAILED
                }
                EhDB.putDownloadInfo(info)
                mDownloadListener?.onFinish(info)
                mutableNotifyFlow.emit(info)
                ensureDownload()
            } ?: logcat(TAG, LogPriority.ERROR) { "Current task is null, but it should not be" }
        }
    }

    override fun onGetImageSuccess(index: Int, image: Image?) {
        // Ignore
    }

    override fun onGetImageFailure(index: Int, error: String?) {
        // Ignore
    }

    interface DownloadListener {
        /**
         * Get 509 error
         */
        fun onGet509()

        /**
         * Start download
         */
        fun onStart(info: DownloadInfo)

        /**
         * Update download speed
         */
        fun onDownload(info: DownloadInfo)

        /**
         * Update page downloaded
         */
        fun onGetPage(info: DownloadInfo)

        /**
         * Download done
         */
        fun onFinish(info: DownloadInfo)

        /**
         * Download done
         */
        fun onCancel(info: DownloadInfo)
    }

    private val mSpeedReminder = object {
        private val mContentLengthMap = SparseLongArray()
        private val mReceivedSizeMap = SparseLongArray()
        private var mBytesRead = 0L
        private var oldSpeed = -1L
        private var currentJob: Job? = null
        fun start() {
            if (currentJob == null) {
                currentJob = launch {
                    while (true) {
                        runInternal()
                    }
                }
            }
        }

        fun stop() {
            mBytesRead = 0
            oldSpeed = -1
            mContentLengthMap.clear()
            mReceivedSizeMap.clear()
            currentJob?.cancel()
            currentJob = null
        }

        fun onDownload(index: Int, contentLength: Long, receivedSize: Long, bytesRead: Int) {
            mContentLengthMap.put(index, contentLength)
            mReceivedSizeMap.put(index, receivedSize)
            mBytesRead += bytesRead.toLong()
        }

        fun onDone(index: Int) {
            mContentLengthMap.delete(index)
            mReceivedSizeMap.delete(index)
        }

        fun onFinish() {
            mContentLengthMap.clear()
            mReceivedSizeMap.clear()
        }

        private suspend fun runInternal() {
            mCurrentTask?.let { info ->
                var newSpeed = mBytesRead / 2
                if (oldSpeed != -1L) {
                    newSpeed = lerp(oldSpeed, newSpeed, 0.75f)
                }
                oldSpeed = newSpeed
                info.speed = newSpeed

                // Calculate remaining
                if (info.total <= 0) {
                    info.remaining = -1
                } else if (newSpeed == 0L) {
                    info.remaining = 300L * 24L * 60L * 60L * 1000L // 300 days
                } else {
                    var downloadingCount = 0
                    var downloadingContentLengthSum = 0L
                    var totalSize = 0L
                    for (i in 0..<mContentLengthMap.size()) {
                        val contentLength = mContentLengthMap.valueAt(i)
                        val receivedSize = mReceivedSizeMap.valueAt(i)
                        downloadingCount++
                        downloadingContentLengthSum += contentLength
                        totalSize += contentLength - receivedSize
                    }
                    if (downloadingCount != 0) {
                        totalSize += downloadingContentLengthSum * (info.total - info.downloaded - downloadingCount) / downloadingCount
                        info.remaining = totalSize / newSpeed * 1000
                    }
                }
                mDownloadListener?.onDownload(info)
                mutableNotifyFlow.emit(info)
            }
            mBytesRead = 0
            delay(2000)
        }
    }

    private const val TAG = "DownloadManager"

    private fun comparator() = SortMode.from(Settings.downloadSortMode.value).comparator()
}

var downloadLocation: UniFile
    get() = with(Settings) {
        val uri = Uri.Builder().apply {
            scheme(downloadScheme)
            encodedAuthority(downloadAuthority)
            encodedPath(downloadPath)
            encodedQuery(downloadQuery)
            encodedFragment(downloadFragment)
        }.build()
        uri.asUniFileOrNull() ?: AppConfig.defaultDownloadDir?.asUniFile() ?: UniFile.Stub
    }
    set(value) = with(value.uri) {
        Settings.edit {
            downloadScheme = scheme
            downloadAuthority = encodedAuthority
            downloadPath = encodedPath
            downloadQuery = encodedQuery
            downloadFragment = encodedFragment
        }
    }

val DownloadInfo.downloadDir get() = dirname?.let { downloadLocation / it }
val DownloadInfo.archiveFile get() = downloadDir?.findFile("$gid.cbz")
