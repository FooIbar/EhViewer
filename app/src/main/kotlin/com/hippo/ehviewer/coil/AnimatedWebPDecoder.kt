package com.hippo.ehviewer.coil

import coil3.ImageLoader
import coil3.asImage
import coil3.decode.ByteBufferMetadata
import coil3.decode.ContentMetadata
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.gif.isAnimatedWebP
import coil3.request.Options
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import okio.FileSystem

class AnimatedWebPDecoder(private val source: ByteBuffer) : Decoder {
    override suspend fun decode() = DecodeResult(AnimatedWebPDrawable(source).asImage(), false)

    object Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ) = if (DecodeUtils.isAnimatedWebP(result.source.source())) {
            result.source.toByteBufferOrNull()?.let { AnimatedWebPDecoder(it) }
        } else {
            null
        }
    }
}

private fun ImageSource.toByteBufferOrNull(): ByteBuffer? {
    if (fileSystem === FileSystem.SYSTEM) {
        val file = fileOrNull()
        if (file != null) {
            return file.toFile().inputStream().mapReadOnly()
        }
    }
    return when (val metadata = metadata) {
        is ContentMetadata -> metadata.assetFileDescriptor.createInputStream().mapReadOnly()
        is ByteBufferMetadata -> metadata.byteBuffer
        else -> null
    }
}

private fun FileInputStream.mapReadOnly(): ByteBuffer = channel.use { it.map(FileChannel.MapMode.READ_ONLY, 0, it.size()) }
