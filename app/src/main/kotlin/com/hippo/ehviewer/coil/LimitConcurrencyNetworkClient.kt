package com.hippo.ehviewer.coil

import coil3.network.NetworkClient
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.ktor3.asNetworkClient
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.URL_PREFIX_THUMB_E
import com.hippo.ehviewer.client.URL_PREFIX_THUMB_EX
import io.ktor.client.HttpClient
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class LimitConcurrencyNetworkClient(val impl: NetworkClient) : NetworkClient {
    val eSemaphore = Semaphore(Settings.thumbConcurrency)
    val exSemaphore = Semaphore(Settings.thumbConcurrency)
    override suspend fun <T> executeRequest(req: NetworkRequest, f: suspend (NetworkResponse) -> T): T {
        val url = req.url
        return when {
            URL_PREFIX_THUMB_E in url -> eSemaphore.withPermit { impl.executeRequest(req, f) }
            URL_PREFIX_THUMB_EX in url -> exSemaphore.withPermit { withContext(NonCancellable) { impl.executeRequest(req, f) } }
            else -> impl.executeRequest(req, f)
        }
    }
}

fun HttpClient.limitConcurrency() = LimitConcurrencyNetworkClient(asNetworkClient())
