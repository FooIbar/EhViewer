package com.hippo.ehviewer.client.parser

import eu.kanade.tachiyomi.util.system.logcat
import io.ktor.util.moveToByteArray
import java.nio.ByteBuffer
import kotlin.system.measureNanoTime
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray

inline fun <reified T> unmarshalParsingAs(body: ByteBuffer, parser: (ByteBuffer, Int) -> Int): T {
    val jsonBytes: Int
    measureNanoTime {
        jsonBytes = parser(body, body.limit())
    }.also { it.logcat { "Parse + marshal use $it ns" } }
    body.limit(jsonBytes)
    val t: T
    measureNanoTime {
        val array = body.moveToByteArray()
        t = Cbor.decodeFromByteArray(array)
    }.also { it.logcat { "Unmarshal + allocate java use $it ns" } }
    return t
}
