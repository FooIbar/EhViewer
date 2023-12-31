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

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.StrictMode
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.collection.LruCache
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.coroutineScope
import coil3.SingletonImageLoader
import coil3.asCoilImage
import coil3.decode.ImageDecoderDecoder
import coil3.fetch.NetworkFetcher
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.coil.DownloadThumbInterceptor
import com.hippo.ehviewer.coil.MergeInterceptor
import com.hippo.ehviewer.dailycheck.checkDawn
import com.hippo.ehviewer.dao.SearchDatabase
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.ktbuilder.diskCache
import com.hippo.ehviewer.ktbuilder.imageLoader
import com.hippo.ehviewer.ktor.CronetEngine
import com.hippo.ehviewer.legacy.cleanObsoleteCache
import com.hippo.ehviewer.ui.keepNoMediaFileStatus
import com.hippo.ehviewer.ui.lockObserver
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.Crash
import com.hippo.ehviewer.util.FavouriteStatusRouter
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.isAtLeastP
import com.hippo.ehviewer.util.isAtLeastQ
import com.hippo.ehviewer.util.isAtLeastS
import com.hippo.ehviewer.util.isCronetAvailable
import com.hippo.ehviewer.util.resettableLazy
import com.hippo.ehviewer.util.unsafeLazy
import eu.kanade.tachiyomi.network.interceptor.UncaughtExceptionInterceptor
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.logcat
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cookies.HttpCookies
import java.io.File
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import okhttp3.AsyncDns
import okhttp3.android.AndroidAsyncDns
import okio.Path.Companion.toOkioPath
import splitties.arch.room.roomDb
import splitties.init.appCtx

private val lifecycle = ProcessLifecycleOwner.get().lifecycle
private val lifecycleScope = lifecycle.coroutineScope

class EhApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        // Initialize Settings on first access
        lifecycleScope.launchIO {
            val mode = Settings.theme
            if (!isAtLeastS) {
                withUIContext {
                    AppCompatDelegate.setDefaultNightMode(mode)
                }
            }
        }
        lifecycle.addObserver(lockObserver)
        val handler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                if (Settings.saveCrashLog) {
                    Crash.saveCrashLog(e)
                }
            } catch (ignored: Throwable) {
            }
            handler?.uncaughtException(t, e)
        }
        super.onCreate()
        if (isAtLeastP) {
            System.loadLibrary("ehviewer")
        }
        System.loadLibrary("ehviewer_rust")
        ReadableTime.initialize(this)
        lifecycleScope.launchIO {
            launchIO {
                EhTagDatabase
            }
            launchIO {
                EhDB
            }
            launchIO {
                DownloadManager.isIdle
            }
            launchIO {
                runSuspendCatching {
                    val files = mutableListOf<File>()
                    AppConfig.externalCrashDir?.listFiles()?.let { files.addAll(it) }
                    AppConfig.externalParseErrorDir?.listFiles()?.let { files.addAll(it) }
                    files.sortByDescending { it.lastModified() }
                    files.forEachIndexed { index, file ->
                        ensureActive()
                        if (index > 9) {
                            file.delete()
                        }
                    }
                }.onFailure {
                    it.printStackTrace()
                }
            }
            launchIO {
                cleanupDownload()
            }
            if (Settings.requestNews) {
                launchIO {
                    checkDawn()
                }
            }
        }
        cleanObsoleteCache(this)
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults()
            Snapshot.registerApplyObserver { anies, _ ->
                logcat(Log.VERBOSE) { anies.toString() }
            }
        }
    }

    private suspend fun cleanupDownload() {
        runCatching {
            keepNoMediaFileStatus()
        }.onFailure {
            it.printStackTrace()
        }
        runCatching {
            clearTempDir()
        }.onFailure {
            it.printStackTrace()
        }
    }

    private fun clearTempDir() {
        var dir = AppConfig.tempDir
        if (null != dir) {
            FileUtils.deleteContent(dir)
        }
        dir = AppConfig.externalTempDir
        if (null != dir) {
            FileUtils.deleteContent(dir)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            galleryDetailCache.evictAll()
        }
    }

    override fun newImageLoader(context: Context) = imageLoader(context) {
        components {
            add(NetworkFetcher.Factory(unsafeLazy { ktorClient }))
            if (isAtLeastP) {
                add { result, options, _ -> ImageDecoderDecoder(result.source, options, false) }
            }
            add(MergeInterceptor)
            add(DownloadThumbInterceptor)
        }
        diskCache(imageCache)
        crossfade(300)
        val drawable = AppCompatResources.getDrawable(appCtx, R.drawable.image_failed)
        if (drawable != null) error(drawable.asCoilImage(true))
        if (BuildConfig.DEBUG) logger(DebugLogger())
    }

    companion object {
        var ktorClient by resettableLazy {
            if (Settings.enableCronet && isCronetAvailable) {
                HttpClient(CronetEngine) {
                    install(HttpCookies) {
                        storage = EhCookieStore
                    }
                }
            } else {
                HttpClient(OkHttp) {
                    engine {
                        config {
                            if (isAtLeastQ) {
                                dns(AsyncDns.toDns(AndroidAsyncDns.IPv4, AndroidAsyncDns.IPv6))
                            }
                            addInterceptor(UncaughtExceptionInterceptor())
                        }
                    }
                    install(HttpCookies) {
                        storage = EhCookieStore
                    }
                }
            }
        }

        var noRedirectKtorClient by resettableLazy {
            HttpClient(ktorClient.engine) {
                followRedirects = false
                install(HttpCookies) {
                    storage = EhCookieStore
                }
            }
        }

        val galleryDetailCache by lazy {
            LruCache<Long, GalleryDetail>(25).also {
                lifecycleScope.launch {
                    FavouriteStatusRouter.globalFlow.collect { (gid, slot) -> it[gid]?.favoriteSlot = slot }
                }
            }
        }

        val imageCache by lazy {
            diskCache {
                directory(appCtx.cacheDir.toOkioPath() / "image_cache")
                maxSizeBytes(Settings.readCacheSize.coerceIn(320, 5120).toLong() * 1024 * 1024)
            }
        }

        val searchDatabase by lazy { roomDb<SearchDatabase>("search_database.db") }
    }
}
