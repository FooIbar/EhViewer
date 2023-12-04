package com.hippo.ehviewer.cronet

import io.ktor.utils.io.pool.DirectByteBufferPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

val pool = DirectByteBufferPool(32)

// Limit thread to 1 since we are async & non-blocking
val cronetDispatcher = Dispatchers.Default.limitedParallelism(1)
val cronetHttpClientExecutor = cronetDispatcher.asExecutor()
