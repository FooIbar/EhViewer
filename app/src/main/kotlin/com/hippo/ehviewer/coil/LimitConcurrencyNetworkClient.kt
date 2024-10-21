package com.hippo.ehviewer.coil

import coil3.network.NetworkClient
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.ktor3.asNetworkClient
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.URL_PREFIX_THUMB_EX
import com.hippo.ehviewer.client.URL_SIGNATURE_THUMB_NORMAL
import io.ktor.client.HttpClient
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import moe.tarsin.coroutines.NamedSemaphore
import moe.tarsin.coroutines.withPermit

class LimitConcurrencyNetworkClient(val impl: NetworkClient) : NetworkClient {
    val semaphores = NamedSemaphore<String>(permits = Settings.thumbConcurrency)
    override suspend fun <T> executeRequest(req: NetworkRequest, f: suspend (NetworkResponse) -> T): T {
        val url = req.url
        return when {
            // Ex thumb server has no h2 multiplexing support
            URL_PREFIX_THUMB_EX in url -> semaphores.withPermit(URL_PREFIX_THUMB_EX) { withContext(NonCancellable) { impl.executeRequest(req, f) } }
            // H@H server has no h2 multiplexing support
            URL_SIGNATURE_THUMB_NORMAL in url -> semaphores.withPermit(url.substringBefore(URL_SIGNATURE_THUMB_NORMAL)) { withContext(NonCancellable) { impl.executeRequest(req, f) } }
            // H2 multiplexing enabled
            else -> impl.executeRequest(req, f)
        }
    }
}

fun HttpClient.limitConcurrency() = LimitConcurrencyNetworkClient(asNetworkClient())
