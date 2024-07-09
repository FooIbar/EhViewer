package com.hippo.ehviewer.cronet

import com.hippo.ehviewer.Settings
import java.io.File
import org.chromium.net.CronetEngine
import splitties.init.appCtx

val cronetHttpClient: CronetEngine = CronetEngine.Builder(appCtx).apply {
    enableBrotli(true)
    setUserAgent(Settings.userAgent)

    // Cache Quic hint only since the real cache mechanism should on Ktor layer
    val cache = File(appCtx.cacheDir, "http_cache").apply { mkdirs() }
    setStoragePath(cache.path)
    enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK_NO_HTTP, 4096)

    addQuicHint("e-hentai.org", 443, 443)
    addQuicHint("forums.e-hentai.org", 443, 443)
    addQuicHint("exhentai.org", 443, 443)
}.build()
