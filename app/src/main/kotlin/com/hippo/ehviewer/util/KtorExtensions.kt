package com.hippo.ehviewer.util

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.charsets.decode
import io.ktor.utils.io.core.ByteReadPacket
import java.nio.charset.CodingErrorAction

suspend fun HttpResponse.bodyAsUtf8Text(): String {
    val decoder = Charsets.UTF_8.newDecoder().apply {
        onMalformedInput(CodingErrorAction.REPLACE)
        onUnmappableCharacter(CodingErrorAction.REPLACE)
    }
    val input = body<ByteReadPacket>()

    return decoder.decode(input)
}
