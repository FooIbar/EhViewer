package com.hippo.ehviewer.legacy

import java.io.File
import splitties.init.appCtx

private val OBSOLETE_CACHE_DIRS = arrayOf(
    "image",
    "thumb",
    "gallery_image",
    "spider_info",
)

fun cleanObsoleteCache() {
    appCtx.deleteDatabase("hosts.db")
    val dir = appCtx.cacheDir
    for (subdir in OBSOLETE_CACHE_DIRS) {
        val file = File(dir, subdir)
        if (file.exists()) {
            file.deleteRecursively()
        }
    }
}
