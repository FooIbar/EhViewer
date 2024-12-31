package com.hippo.ehviewer.spider

import com.hippo.files.read
import com.hippo.files.write
import io.ktor.utils.io.core.writeFully
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import okio.Path.Companion.toOkioPath
import splitties.init.appCtx

@Serializable
class HAtHBlacklist(val blacklist: MutableMap<String, Instant> = mutableMapOf())

private val cache = appCtx.cacheDir.toOkioPath() / "H@H_blacklist"

val blacklist by lazy {
    runCatching {
        cache.read { Cbor.decodeFromByteArray<HAtHBlacklist>(readByteArray()) }
    }.getOrElse {
        cache.write { HAtHBlacklist().also { writeFully(Cbor.encodeToByteArray(it)) } }
    }
}

fun appendHAtHBlacklist(host: String) = synchronized(blacklist) {
    blacklist.blacklist[host] = Clock.System.now()
    cache.write { writeFully(Cbor.encodeToByteArray(blacklist)) }
}
