package com.hippo.ehviewer.ktor

import com.hippo.ehviewer.util.isAtLeastQ
import io.ktor.client.engine.apache5.Apache5EngineConfig
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.core5.reactor.ssl.SSLBufferMode
import org.apache.hc.core5.ssl.SSLContexts
import org.apache.hc.core5.util.Timeout

fun Apache5EngineConfig.configureClient() {
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
                if (!isAtLeastQ) {
                    val context = SSLContexts.createSystemDefault()
                    setTlsStrategy { session, endpoint, _, _, attachment, timeout ->
                        session.startTls(
                            context,
                            endpoint,
                            SSLBufferMode.STATIC,
                            { _, _ -> },
                            { _, _ -> null },
                            timeout,
                        )
                        true
                    }
                }
            }.build(),
        )
    }
}
