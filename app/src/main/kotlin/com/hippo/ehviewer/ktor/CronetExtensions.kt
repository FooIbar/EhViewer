@file:RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)

package com.hippo.ehviewer.ktor

import android.net.http.HttpEngine
import android.os.Build
import androidx.annotation.RequiresExtension
import java.io.File

fun CronetConfig.configureClient(enableQuic: Boolean) {
    config = {
        setEnableBrotli(true)

        // Cache Quic hint only since the real cache mechanism should on Ktor layer
        val cache = File(context.cacheDir, "http_cache").apply { mkdirs() }
        setStoragePath(cache.path)
        setEnableHttpCache(HttpEngine.Builder.HTTP_CACHE_DISK_NO_HTTP, 4096)

        setEnableQuic(enableQuic)
        if (enableQuic) {
            addQuicHint("e-hentai.org", 443, 443)
            addQuicHint("forums.e-hentai.org", 443, 443)
            addQuicHint("exhentai.org", 443, 443)
        }
    }
}
