package com.hippo.ehviewer.coil

import coil3.network.HttpException
import coil3.network.NetworkClient
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.flipThumbSite

class ExchangeSiteNetworkClient(val impl: NetworkClient) : NetworkClient {
    override suspend fun <T> executeRequest(req: NetworkRequest, f: suspend (NetworkResponse) -> T): T {
        if (EhUtils.isExHentai) {
            try {
                return impl.executeRequest(req, f)
            } catch (e: HttpException) {
                if (e.response.code == 500) {
                    return impl.executeRequest(req.copy(url = flipThumbSite(req.url)), f)
                }
                throw e
            }
        } else {
            return impl.executeRequest(req, f)
        }
    }
}

fun NetworkClient.exchangeSite() = ExchangeSiteNetworkClient(this)
