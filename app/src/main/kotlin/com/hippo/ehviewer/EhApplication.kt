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
import android.content.Context
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.coroutineScope
import coil3.EventListener
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.ConnectivityChecker
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.serviceLoaderEnabled
import coil3.util.DebugLogger
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.coil.CropBorderInterceptor
import com.hippo.ehviewer.coil.DetectBorderInterceptor
import com.hippo.ehviewer.coil.DownloadThumbInterceptor
import com.hippo.ehviewer.coil.HardwareBitmapInterceptor
import com.hippo.ehviewer.coil.MapExtraInfoInterceptor
import com.hippo.ehviewer.coil.MergeInterceptor
import com.hippo.ehviewer.coil.QrCodeInterceptor
import com.hippo.ehviewer.dailycheck.checkDawn
import com.hippo.ehviewer.dao.SearchDatabase
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadsFilterMode
import com.hippo.ehviewer.ktbuilder.diskCache
import com.hippo.ehviewer.ktbuilder.imageLoader
import com.hippo.ehviewer.ktor.Cronet
import com.hippo.ehviewer.ktor.configureClient
import com.hippo.ehviewer.ktor.configureCommon
import com.hippo.ehviewer.ui.keepNoMediaFileStatus
import com.hippo.ehviewer.ui.lockObserver
import com.hippo.ehviewer.ui.screen.detailCache
import com.hippo.ehviewer.ui.tools.dataStateFlow
import com.hippo.ehviewer.ui.tools.initSETConnection
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.CrashHandler
import com.hippo.ehviewer.util.FavouriteStatusRouter
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.OSUtils
import com.hippo.ehviewer.util.isAtLeastO
import com.hippo.ehviewer.util.isAtLeastP
import com.hippo.ehviewer.util.isAtLeastS
import com.hippo.ehviewer.util.isAtLeastSExtension7
import com.hippo.files.deleteContent
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.logcat
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import logcat.asLog
import okio.Path.Companion.toOkioPath
import splitties.arch.room.roomDb
import splitties.init.appCtx

private val lifecycle = ProcessLifecycleOwner.get().lifecycle
private val lifecycleScope = lifecycle.coroutineScope

class EhApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun onCreate() {
        initSETConnection()
        // Initialize Settings on first access
        lifecycleScope.launchIO {
            val mode = Settings.theme
            if (!isAtLeastS) {
                withUIContext {
                    AppCompatDelegate.setDefaultNightMode(mode)
                }
            }
            if (!LogcatLogger.isInstalled && Settings.saveCrashLog) {
                LogcatLogger.install(AndroidLogcatLogger(LogPriority.VERBOSE))
            }
        }
        lifecycle.addObserver(lockObserver)
        CrashHandler.install()
        super.onCreate()
        System.loadLibrary("ehviewer")
        lifecycleScope.launchIO {
            launchUI {
                FavouriteStatusRouter.collect { info ->
                    detailCache[info.gid]?.apply {
                        favoriteSlot = info.favoriteSlot
                        favoriteName = info.favoriteName
                        favoriteNote = info.favoriteNote
                    }
                }
            }
            EhTagDatabase.launchUpdate()
            launch { EhDB }
            launchIO { dataStateFlow.value }
            launchIO { OSUtils.totalMemory }
            launch {
                if (DownloadManager.labelList.isNotEmpty() && Settings.downloadFilterMode.key !in Settings.prefs) {
                    Settings.downloadFilterMode.value = DownloadsFilterMode.CUSTOM.flag
                }
                DownloadManager.readMetadataFromLocal()
            }
            launch {
                FileUtils.cleanupDirectory(AppConfig.externalCrashDir)
                FileUtils.cleanupDirectory(AppConfig.externalParseErrorDir)
            }
            launch { cleanupDownload() }
            if (Settings.requestNews) {
                launch { checkDawn() }
            }
        }
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults()
        }
    }

    private suspend fun cleanupDownload() {
        runCatching {
            keepNoMediaFileStatus()
        }.onFailure {
            logcat(it)
        }
        runCatching {
            clearTempDir()
        }.onFailure {
            logcat(it)
        }
    }

    private fun clearTempDir() {
        AppConfig.tempDir.deleteContent()
        AppConfig.externalTempDir?.deleteContent()
    }

    override fun newImageLoader(context: Context) = context.imageLoader {
        interceptorCoroutineContext(Dispatchers.Default)
        components {
            serviceLoaderEnabled(false)
            add(
                KtorNetworkFetcherFactory(
                    httpClient = { ktorClient },
                    connectivityChecker = { ConnectivityChecker.ONLINE },
                ),
            )
            add(MergeInterceptor)
            add(DownloadThumbInterceptor)
            if (isAtLeastO) {
                add(HardwareBitmapInterceptor)
            } else {
                allowRgb565(true)
            }
            add(CropBorderInterceptor)
            add(DetectBorderInterceptor)
            add(QrCodeInterceptor)
            add(MapExtraInfoInterceptor)
            if (isAtLeastP) {
                add(AnimatedImageDecoder.Factory(false))
            } else {
                add(GifDecoder.Factory())
            }
        }
        diskCache { imageCache }
        crossfade(300)
        val drawable = AppCompatResources.getDrawable(appCtx, R.drawable.image_failed)
        if (drawable != null) error(drawable.asImage(true))
        if (BuildConfig.DEBUG) {
            logger(DebugLogger())
        } else {
            eventListener(object : EventListener() {
                override fun onError(request: ImageRequest, result: ErrorResult) {
                    logcat("ImageLoader", LogPriority.ERROR) {
                        "🚨 Failed - ${request.data}\n${result.throwable.asLog()}"
                    }
                }
            })
        }
    }

    companion object {
        val ktorClient by lazy {
            if (isAtLeastSExtension7 && Settings.enableCronet.value) {
                HttpClient(Cronet) {
                    engine { configureClient() }
                    configureCommon()
                }
            } else {
                HttpClient(OkHttp) {
                    engine { configureClient() }
                    configureCommon()
                }
            }
        }

        val noRedirectKtorClient by lazy {
            HttpClient(ktorClient.engine) {
                configureCommon(redirect = false)
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
