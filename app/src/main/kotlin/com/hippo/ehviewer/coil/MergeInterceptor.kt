package com.hippo.ehviewer.coil

import arrow.core.merge
import arrow.fx.coroutines.raceN
import coil3.decode.DataSource
import coil3.imageLoader
import coil3.intercept.Interceptor
import coil3.memory.MemoryCache
import coil3.request.ImageResult
import coil3.request.SuccessResult
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import moe.tarsin.coroutines.Counter
import moe.tarsin.coroutines.Tracker
import moe.tarsin.coroutines.counter
import moe.tarsin.coroutines.runSuspendCatching
import moe.tarsin.coroutines.use

typealias F = suspend () -> Unit

suspend inline fun <T> CancellableContinuation<T>.evalAndResume(f: suspend () -> T) = runSuspendCatching { f() }.takeIf { isActive }?.let(::resumeWith)

object SequentialFunction : Tracker<ContinuationFlow, String>() {
    override fun new() = ContinuationFlow()
    override fun free(e: ContinuationFlow) = e.cancel()
}

class ContinuationFlow : CoroutineScope, Counter by counter() {
    override val coroutineContext = Dispatchers.IO + Job()
    val actions = MutableSharedFlow<F>()
    val flow = actions.map(F::invoke).shareIn(this, SharingStarted.WhileSubscribed())

    suspend inline fun <R> sendAndAwait(crossinline block: suspend () -> R) = raceN(
        { suspendCancellableCoroutine { cont -> launch { actions.emit { cont.evalAndResume(block) } } } },
        { flow.collect {} },
    ).merge()
}

object MergeInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val req = chain.request
        val key = req.memoryCacheKey
        return if (key != null) {
            val cacheKey = MemoryCache.Key(key, req.memoryCacheKeyExtras)
            val cached = req.context.imageLoader.memoryCache!![cacheKey] != null
            val result = SequentialFunction.use(key) { sendAndAwait { chain.proceed() } }
            when (result) {
                is SuccessResult if cached -> result.copy(dataSource = DataSource.MEMORY)
                else -> result
            }
        } else {
            chain.proceed()
        }
    }
}
