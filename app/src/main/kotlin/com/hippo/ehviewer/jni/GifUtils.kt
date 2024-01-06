package com.hippo.ehviewer.jni

import java.nio.ByteBuffer

external fun isGif(fd: Int): Boolean
external fun rewriteGifSource(buffer: ByteBuffer)
external fun mmap(fd: Int): ByteBuffer?
external fun munmap(buffer: ByteBuffer)
