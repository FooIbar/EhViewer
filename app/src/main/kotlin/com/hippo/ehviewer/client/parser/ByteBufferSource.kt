package com.hippo.ehviewer.client.parser

import io.ktor.util.moveToByteArray
import java.nio.ByteBuffer
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf

inline fun <reified T> unmarshalParsingAs(body: ByteBuffer, parser: (ByteBuffer, Int) -> Int): T {
    val len = parser(body, body.limit())
    body.limit(len)
    val array = body.moveToByteArray()
    return ProtoBuf.decodeFromByteArray<T>(array)
}
