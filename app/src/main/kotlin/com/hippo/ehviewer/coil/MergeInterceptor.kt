package com.hippo.ehviewer.coil

import androidx.collection.mutableScatterMapOf
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
import moe.tarsin.coroutines.Pool
import moe.tarsin.coroutines.counter
import moe.tarsin.coroutines.use

typealias F = suspend () -> Unit

suspend inline fun <T> CancellableContinuation<T>.evalAndResume(f: suspend () -> T) = runCatching { f() }.takeIf { isActive }?.let(::resumeWith)

object SequentialFunction : Pool<ContinuationFlow, String> {
    val active = mutableScatterMapOf<String, ContinuationFlow>()
    override fun acquire(key: String) = synchronized(active) { active.getOrPut(key) { ContinuationFlow() }.inc() }

    override fun release(key: String, lock: ContinuationFlow) = synchronized(active) {
        lock.dec()
        if (lock.isFree) {
            lock.cancel()
            active.remove(key)
        }
    }
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
