package com.hippo.ehviewer.client.parser

import io.ktor.util.moveToByteArray
import java.nio.ByteBuffer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray

inline fun <reified T> unmarshalParsingAs(body: ByteBuffer, parser: (ByteBuffer, Int) -> Int): T {
    val cborBytes = parser(body, body.limit())
    body.limit(cborBytes)
    val array = body.moveToByteArray()
    return Cbor.decodeFromByteArray<T>(array)
}
