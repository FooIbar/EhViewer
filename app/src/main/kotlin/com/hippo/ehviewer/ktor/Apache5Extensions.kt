package com.hippo.ehviewer.ktor

import com.hippo.ehviewer.util.isAtLeastQ
import io.ktor.client.engine.apache5.Apache5EngineConfig
import java.security.Security
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.core5.reactor.IOReactorConfig
import org.apache.hc.core5.util.Timeout
import org.conscrypt.Conscrypt

fun Apache5EngineConfig.configureClient() {
    if (!isAtLeastQ) {
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }
    customizeClient {
        setConnectionManager(
            PoolingAsyncClientConnectionManagerBuilder.create().apply {
                setMaxConnPerRoute(2)
                setDefaultConnectionConfig(
                    ConnectionConfig.custom().apply {
                        setConnectTimeout(Timeout.ofMilliseconds(connectTimeout))
                        setSocketTimeout(Timeout.ofMilliseconds(socketTimeout.toLong()))
                    }.build(),
                )
                setIOReactorConfig(
                    IOReactorConfig.custom().apply {
                        setIoThreadCount(1)
                    }.build(),
                )
            }.build(),
        )
    }
}
