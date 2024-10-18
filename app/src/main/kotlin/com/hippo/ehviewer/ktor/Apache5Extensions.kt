package com.hippo.ehviewer.ktor

import com.hippo.ehviewer.util.isAtLeastQ
import io.ktor.client.engine.apache5.Apache5EngineConfig
import java.net.SocketAddress
import moe.tarsin.kt.unreachable
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.nio.ssl.TlsStrategy
import org.apache.hc.core5.http.ssl.TLS
import org.apache.hc.core5.http2.ssl.ApplicationProtocol
import org.apache.hc.core5.http2.ssl.H2TlsSupport
import org.apache.hc.core5.net.NamedEndpoint
import org.apache.hc.core5.reactor.IOReactorConfig
import org.apache.hc.core5.reactor.ssl.SSLBufferMode
import org.apache.hc.core5.reactor.ssl.TransportSecurityLayer
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
                setIOReactorConfig(
                    IOReactorConfig.custom().apply {
                        setIoThreadCount(1)
                    }.build(),
                )
                if (!isAtLeastQ) {
                    // TLS v1.3 support started from Android 10
                    val tlsVersions = arrayOf(TLS.V_1_2.id)
                    val protocols = arrayOf(ApplicationProtocol.HTTP_2.id, ApplicationProtocol.HTTP_1_1.id)
                    val context = SSLContexts.createSystemDefault()
                    setTlsStrategy(
                        object : TlsStrategy {
                            override fun upgrade(s: TransportSecurityLayer, h: HttpHost?, l: SocketAddress?, r: SocketAddress?, a: Any?, t: Timeout?) = unreachable()

                            override fun upgrade(session: TransportSecurityLayer, endpoint: NamedEndpoint?, attachment: Any?, timeout: Timeout?, callback: FutureCallback<TransportSecurityLayer?>) {
                                session.startTls(
                                    context,
                                    endpoint,
                                    SSLBufferMode.STATIC,
                                    { _, engine ->
                                        val params = engine.sslParameters
                                        params.protocols = tlsVersions
                                        H2TlsSupport.setApplicationProtocols(params, protocols)
                                        H2TlsSupport.setEnableRetransmissions(params, false)
                                        engine.sslParameters = params
                                    },
                                    { _, _ -> null },
                                    timeout,
                                    callback,
                                )
                            }
                        },
                    )
                }
            }.build(),
        )
    }
}
