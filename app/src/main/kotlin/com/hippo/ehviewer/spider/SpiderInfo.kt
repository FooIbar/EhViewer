package com.hippo.ehviewer.spider

import com.hippo.ehviewer.coil.edit
import com.hippo.ehviewer.coil.read
import com.hippo.ehviewer.ktbuilder.diskCache
import com.hippo.ehviewer.legacy.readLegacySpiderInfo
import com.hippo.unifile.UniFile
import com.hippo.unifile.openInputStream
import com.hippo.unifile.openOutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import moe.tarsin.coroutines.runSuspendCatching
import okio.Path.Companion.toOkioPath
import splitties.init.appCtx

@Serializable
class SpiderInfo(
    val gid: Long,

    val token: String,

    val pages: Int,

    val pTokenMap: MutableMap<Int, String> = hashMapOf(),

    var previewPages: Int = -1,

    var previewPerPage: Int = -1,
)

private val cbor = Cbor {
    ignoreUnknownKeys = true
}

fun SpiderInfo.write(file: UniFile) {
    file.openOutputStream().use {
        it.write(cbor.encodeToByteArray(this))
    }
}

fun SpiderInfo.saveToCache() {
    runSuspendCatching {
        spiderInfoCache.edit(gid.toString()) {
            data.toFile().writeBytes(cbor.encodeToByteArray(this@saveToCache))
        }
    }.onFailure {
        it.printStackTrace()
    }
}

private val spiderInfoCache by lazy {
    diskCache {
        directory(appCtx.cacheDir.toOkioPath() / "spider_info_v2_1")
        maxSizeBytes(20 * 1024 * 1024)
    }
}

fun readFromCache(gid: Long): SpiderInfo? {
    return spiderInfoCache.read(gid.toString()) {
        runCatching {
            cbor.decodeFromByteArray<SpiderInfo>(data.toFile().readBytes())
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }
}

fun readCompatFromUniFile(file: UniFile): SpiderInfo? {
    return runCatching {
        file.openInputStream().use {
            cbor.decodeFromByteArray<SpiderInfo>(it.readBytes())
        }
    }.getOrNull() ?: runCatching {
        file.openInputStream().use { readLegacySpiderInfo(it) }
    }.getOrNull()
}

const val TOKEN_FAILED = "failed"
