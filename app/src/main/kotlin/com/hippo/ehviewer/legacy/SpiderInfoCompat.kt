package com.hippo.ehviewer.legacy

import com.hippo.ehviewer.spider.SpiderInfo
import com.hippo.files.read
import kotlinx.io.readLineStrict
import okio.Path

fun Path.readLegacySpiderInfo() = read {
    fun read() = readLineStrict()
    fun readInt() = read().toInt()
    fun readLong() = read().toLong()

    read() // Skip version, we assert it's v2
    read() // Skip startPage
    val gid = readLong()
    val token = read()
    read() // Skip mode
    read() // Skip previewPages
    val previewPerPage = readInt()
    val pages = readInt()
    SpiderInfo(gid, token, pages, previewPerPage = previewPerPage).apply {
        runCatching {
            while (true) {
                val line = read()
                val pos = line.indexOf(" ")
                if (pos > 0) {
                    val index = line.substring(0, pos).toInt()
                    val pToken = line.substring(pos + 1)
                    if (pToken.isNotEmpty()) {
                        pTokenMap[index] = pToken
                    }
                }
            }
        }
    }
}
