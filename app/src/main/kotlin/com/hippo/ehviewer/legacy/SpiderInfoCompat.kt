package com.hippo.ehviewer.legacy

import com.hippo.ehviewer.spider.SpiderInfo
import com.hippo.files.inputStream
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.readLineStrict
import okio.Path

fun Path.readLegacySpiderInfo() = inputStream().asSource().buffered().use { source ->
    fun read(): String = source.readLineStrict()
    fun readInt(): Int = read().toInt()
    fun readLong(): Long = read().toLong()

    repeat(2) { read() } // We assert that only info v2
    val gid = readLong()
    val token = read()
    read()
    val previewPages = readInt()
    val previewPerPage = readInt()
    val pages = read().toInt()
    val pTokenMap = hashMapOf<Int, String>()
    SpiderInfo(gid, token, pages, pTokenMap, previewPages, previewPerPage).apply {
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
