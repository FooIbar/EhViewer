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

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.collection.LongSparseArray
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.getParcelableExtraCompat
import com.hippo.ehviewer.util.unsafeLazy
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

class DownloadService :
    Service(),
    DownloadManager.DownloadListener,
    CoroutineScope {
    override val coroutineContext = Dispatchers.IO + SupervisorJob()
    private val deferredMgr = async { DownloadManager }
    private val notifyManager by unsafeLazy { NotificationManagerCompat.from(this) }
    private val downloadingNotification by unsafeLazy { initDownloadingNotification() }
    private val downloadedNotification by lazy { initDownloadedNotification() }
    private val error509Notification by lazy { init509Notification() }
    private val channelId by unsafeLazy { "$packageName.download" }

    override fun onCreate() {
        notifyManager.createNotificationChannel(
            NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.download_service)).build(),
        )
        downloadingNotification.builder.run {
            setContentTitle(getString(R.string.download_service))
                .setContentText(null)
                .setSubText(null)
                .setProgress(0, 0, true)
            startForeground(ID_DOWNLOADING, build())
        }
        launch {
            deferredMgr.await().setDownloadListener(this@DownloadService)
        }
    }

    override fun onDestroy() {
        val scope = this
        launch {
            deferredMgr.await().setDownloadListener(null)
            // Wait for the last notification to be posted
            delay(DELAY)
            scope.cancel()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        launch { handleIntent(intent) }
        return START_STICKY
    }

    private suspend fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_START -> {
                val gi = intent.getParcelableExtraCompat<BaseGalleryInfo>(KEY_GALLERY_INFO)
                val label = intent.getStringExtra(KEY_LABEL)
                if (gi != null) {
                    deferredMgr.await().startDownload(gi, label)
                }
            }

            ACTION_START_RANGE -> {
                val gidList = intent.getLongArrayExtra(KEY_GID_LIST)
                if (gidList != null) {
                    deferredMgr.await().startRangeDownload(gidList)
                }
            }

            ACTION_START_ALL -> {
                deferredMgr.await().startAllDownload()
            }

            ACTION_STOP -> {
                val gid = intent.getLongExtra(KEY_GID, -1)
                if (gid != -1L) {
                    deferredMgr.await().stopDownload(gid)
                }
            }

            ACTION_STOP_CURRENT -> deferredMgr.await().stopCurrentDownload()

            ACTION_STOP_RANGE -> {
                val gidList = intent.getLongArrayExtra(KEY_GID_LIST)
                if (gidList != null) {
                    deferredMgr.await().stopRangeDownload(gidList)
                }
            }

            ACTION_STOP_ALL -> deferredMgr.await().stopAllDownload()

            ACTION_DELETE -> {
                val gid = intent.getLongExtra(KEY_GID, -1)
                if (gid != -1L) {
                    deferredMgr.await().deleteDownload(gid)
                }
            }

            ACTION_DELETE_RANGE -> {
                val gidList = intent.getLongArrayExtra(KEY_GID_LIST)
                if (gidList != null) {
                    deferredMgr.await().deleteRangeDownload(gidList)
                }
            }

            ACTION_CLEAR -> {
                clear()
                checkStopSelf()
            }
        }
    }

    override fun onBind(intent: Intent) = null

    private fun initDownloadingNotification(): NotificationHandler {
        val stopAllIntent = Intent(this, DownloadService::class.java)
        stopAllIntent.action = ACTION_STOP_ALL
        val piStopAll =
            PendingIntent.getService(this, 0, stopAllIntent, PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .addAction(
                R.drawable.v_pause_x24,
                getString(R.string.stat_download_action_stop_all),
                piStopAll,
            )
            .setShowWhen(false)
        return NotificationHandler(this, notifyManager, builder, ID_DOWNLOADING)
            .apply { launch { run() } }
    }

    private fun initDownloadedNotification(): NotificationHandler {
        val clearIntent = Intent(this, DownloadService::class.java)
        clearIntent.action = ACTION_CLEAR
        val piClear = PendingIntent.getService(this, 0, clearIntent, PendingIntent.FLAG_IMMUTABLE)
        val bundle = Bundle()
        bundle.putString(KEY_ACTION, ACTION_CLEAR_DOWNLOAD_SERVICE)
        val activityIntent = Intent(this, MainActivity::class.java)
        activityIntent.action = ACTION_START_DOWNLOADSCENE
        activityIntent.putExtra(ACTION_START_DOWNLOADSCENE_ARGS, bundle)
        val piActivity = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(getString(R.string.stat_download_done_title))
            .setDeleteIntent(piClear)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(piActivity)
        return NotificationHandler(this, notifyManager, builder, ID_DOWNLOADED)
            .apply { launch { run() } }
    }

    private fun init509Notification(): NotificationHandler {
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_baseline_warning_24)
            .setContentTitle(getString(R.string.stat_509_alert_title))
            .setContentText(getString(R.string.stat_509_alert_text))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(getString(R.string.stat_509_alert_text)),
            )
            .setAutoCancel(true)
            .setOngoing(false)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
        return NotificationHandler(this, notifyManager, builder, ID_509)
            .apply { launch { run() } }
    }

    override fun onGet509() {
        error509Notification.run {
            builder.setWhen(System.currentTimeMillis())
            show()
        }
    }

    override fun onStart(info: DownloadInfo) {
        val bundle = Bundle()
        bundle.putLong(KEY_GID, info.gid)
        val activityIntent = Intent(this, MainActivity::class.java)
        activityIntent.action = ACTION_START_DOWNLOADSCENE
        activityIntent.putExtra(ACTION_START_DOWNLOADSCENE_ARGS, bundle)
        val piActivity = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        downloadingNotification.run {
            builder.setContentTitle(EhUtils.getSuitableTitle(info))
                .setContentText(null)
                .setSubText(null)
                .setProgress(0, 0, true)
                .setContentIntent(piActivity)
            startForeground()
        }
    }

    override fun onDownload(info: DownloadInfo) {
        val speed = info.speed.coerceAtLeast(0)
        val speedText = FileUtils.humanReadableByteCount(speed, false) + "/s"
        val remaining = info.remaining
        val text = if (remaining >= 0) {
            val interval = ReadableTime.getShortTimeInterval(remaining)
            getString(R.string.download_speed_text_2, speedText, interval)
        } else {
            getString(R.string.download_speed_text, speedText)
        }
        downloadingNotification.run {
            builder.setContentTitle(EhUtils.getSuitableTitle(info))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setSubText(if (info.total == -1 || info.finished == -1) null else info.finished.toString() + "/" + info.total)
                .setProgress(info.total, info.finished, false)
            startForeground()
        }
    }

    override fun onFinish(info: DownloadInfo) {
        downloadingNotification.cancel()
        val finish = info.state == DownloadInfo.STATE_FINISH
        val gid = info.gid
        val index = sItemStateArray.indexOfKey(gid)
        if (index < 0) { // Not contain
            sItemStateArray.put(gid, finish)
            sItemTitleArray.put(gid, EhUtils.getSuitableTitle(info))
            sDownloadedCount++
            if (finish) {
                sFinishedCount++
            } else {
                sFailedCount++
            }
        } else { // Contain
            val oldFinish = sItemStateArray.valueAt(index)
            sItemStateArray.put(gid, finish)
            sItemTitleArray.put(gid, EhUtils.getSuitableTitle(info))
            if (oldFinish && !finish) {
                sFinishedCount--
                sFailedCount++
            } else if (!oldFinish && finish) {
                sFinishedCount++
                sFailedCount--
            }
        }
        val text: String
        val needStyle: Boolean
        if (sFinishedCount != 0 && sFailedCount == 0) {
            if (sFinishedCount == 1) {
                text = if (sItemTitleArray.size() >= 1) {
                    getString(
                        R.string.stat_download_done_line_succeeded,
                        sItemTitleArray.valueAt(0),
                    )
                } else {
                    logcat { "WTF, sItemTitleArray is null" }
                    getString(R.string.error_unknown)
                }
                needStyle = false
            } else {
                text = getString(R.string.stat_download_done_text_succeeded, sFinishedCount)
                needStyle = true
            }
        } else if (sFinishedCount == 0 && sFailedCount != 0) {
            if (sFailedCount == 1) {
                text = if (sItemTitleArray.size() >= 1) {
                    getString(
                        R.string.stat_download_done_line_failed,
                        sItemTitleArray.valueAt(0),
                    )
                } else {
                    logcat { "WTF, sItemTitleArray is null" }
                    getString(R.string.error_unknown)
                }
                needStyle = false
            } else {
                text = getString(R.string.stat_download_done_text_failed, sFailedCount)
                needStyle = true
            }
        } else {
            text = getString(R.string.stat_download_done_text_mix, sFinishedCount, sFailedCount)
            needStyle = true
        }
        val style: NotificationCompat.InboxStyle?
        if (needStyle) {
            style = NotificationCompat.InboxStyle()
            style.setBigContentTitle(getString(R.string.stat_download_done_title))
            val stateArray = sItemStateArray
            var i = 0
            val n = stateArray.size()
            while (i < n) {
                val id = stateArray.keyAt(i)
                val fin = stateArray.valueAt(i)
                val title = sItemTitleArray[id]
                if (title == null) {
                    i++
                    continue
                }
                style.addLine(
                    getString(
                        if (fin) R.string.stat_download_done_line_succeeded else R.string.stat_download_done_line_failed,
                        title,
                    ),
                )
                i++
            }
        } else {
            style = null
        }
        downloadedNotification.run {
            builder.setContentText(text)
                .setStyle(style)
                .setWhen(System.currentTimeMillis())
                .setNumber(sDownloadedCount)
            show()
        }
        checkStopSelf()
    }

    override fun onCancel(info: DownloadInfo) {
        downloadingNotification.cancel()
        checkStopSelf()
    }

    private fun checkStopSelf() {
        launch {
            if (deferredMgr.await().isIdle) {
                ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private class NotificationHandler(
        private val service: Service,
        private val notifyManager: NotificationManagerCompat,
        val builder: NotificationCompat.Builder,
        private val id: Int,
    ) {
        private val channel = Channel<Ops>(Channel.CONFLATED)

        fun show() {
            channel.trySend(Ops.Notify)
        }

        fun cancel() {
            channel.trySend(Ops.Cancel)
        }

        fun startForeground() {
            channel.trySend(Ops.StartForeground)
        }

        suspend fun run() {
            channel.receiveAsFlow().sample(DELAY).collect {
                when (it) {
                    Ops.Notify -> {
                        if (ActivityCompat.checkSelfPermission(
                                service,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            notifyManager.notify(id, builder.build())
                        }
                    }

                    Ops.Cancel -> notifyManager.cancel(id)
                    Ops.StartForeground -> service.startForeground(id, builder.build())
                }
            }
        }

        private enum class Ops {
            Notify,
            Cancel,
            StartForeground,
        }
    }

    companion object {
        const val ACTION_START_DOWNLOADSCENE = "start_download_scene"
        const val ACTION_START_DOWNLOADSCENE_ARGS = "start_download_scene_args"

        const val ACTION_START = "start"
        const val ACTION_START_RANGE = "start_range"
        const val ACTION_START_ALL = "start_all"
        const val ACTION_STOP = "stop"
        const val ACTION_STOP_RANGE = "stop_range"
        const val ACTION_STOP_CURRENT = "stop_current"
        const val ACTION_STOP_ALL = "stop_all"
        const val ACTION_DELETE = "delete"
        const val ACTION_DELETE_RANGE = "delete_range"
        const val ACTION_CLEAR = "clear"
        const val KEY_GALLERY_INFO = "gallery_info"
        const val KEY_LABEL = "label"
        const val KEY_GID = "gid"
        const val KEY_GID_LIST = "gid_list"
        const val KEY_ACTION = "action"
        const val ACTION_CLEAR_DOWNLOAD_SERVICE = "clear_download_service"
        private const val ID_DOWNLOADING = 1
        private const val ID_DOWNLOADED = 2
        private const val ID_509 = 3
        private const val DELAY = 1000L // 1s
        private val sItemStateArray = LongSparseArray<Boolean>()
        private val sItemTitleArray = LongSparseArray<String>()
        private var sFailedCount = 0
        private var sFinishedCount = 0
        private var sDownloadedCount = 0

        fun clear() {
            sFailedCount = 0
            sFinishedCount = 0
            sDownloadedCount = 0
            sItemStateArray.clear()
            sItemTitleArray.clear()
        }
    }
}
