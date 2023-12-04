@file:Suppress("NewApi")

package com.hippo.ehviewer.cronet

import android.net.http.HttpEngine
import com.hippo.ehviewer.Settings
import okio.Path.Companion.toOkioPath
import splitties.init.appCtx

val cronetHttpClient: HttpEngine = HttpEngine.Builder(appCtx).apply {
    setEnableBrotli(true)
    setUserAgent(Settings.userAgent)

    // Cache Quic hint only since the real cache mechanism should on Ktor layer
    val cache = (appCtx.cacheDir.toOkioPath() / "http_cache").toFile().apply { mkdirs() }
    setStoragePath(cache.absolutePath)
    setEnableHttpCache(HttpEngine.Builder.HTTP_CACHE_DISK_NO_HTTP, 4096)

    addQuicHint("e-hentai.org", 443, 443)
    addQuicHint("forums.e-hentai.org", 443, 443)
    addQuicHint("exhentai.org", 443, 443)
}.build()
