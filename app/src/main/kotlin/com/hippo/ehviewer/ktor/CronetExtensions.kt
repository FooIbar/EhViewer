package com.hippo.ehviewer.ktor

import android.net.http.HttpEngine
import android.os.Build
import androidx.annotation.RequiresExtension
import com.ehviewer.core.util.isAtLeastSExtension7
import java.io.File

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
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

val isCronetAvailable: Boolean
    get() = isAtLeastSExtension7 && !isDeviceBlocked

// https://github.com/FooIbar/EhViewer/issues/1826
private val isDeviceBlocked = when (Build.VERSION.INCREMENTAL.substringBefore('.')) {
    "V816", // HyperOS 1
    "OS2", // HyperOS 2
    -> true
    else -> false
}
