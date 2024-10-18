package com.hippo.ehviewer.ktor

import io.ktor.client.engine.apache5.Apache5EngineConfig
import java.security.Security
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.core5.util.Timeout
import org.conscrypt.Conscrypt

fun Apache5EngineConfig.configureClient() {
    Security.insertProviderAt(Conscrypt.newProvider(), 1)
    customizeClient {
        setConnectionManager(
            PoolingAsyncClientConnectionManagerBuilder.create()
                .setMaxConnTotal(MAX_CONNECTIONS_COUNT)
                .setMaxConnPerRoute(MAX_CONNECTIONS_COUNT)
                .setDefaultConnectionConfig(
                    ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout))
                        .setSocketTimeout(Timeout.ofMilliseconds(socketTimeout.toLong()))
                        .build(),
                )
                .build(),
        )
    }
}

private const val MAX_CONNECTIONS_COUNT = 1000
