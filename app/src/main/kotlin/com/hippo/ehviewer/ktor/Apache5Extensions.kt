package com.hippo.ehviewer.ktor

import android.net.DnsResolver
import android.os.Build
import androidx.annotation.RequiresApi
import arrow.fx.coroutines.parZip
import com.hippo.ehviewer.util.isAtLeastQ
import io.ktor.client.engine.apache5.Apache5EngineConfig
import java.net.InetAddress
import java.net.UnknownHostException
import java.security.Security
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import moe.tarsin.kt.unreachable
import org.apache.hc.client5.http.DnsResolver as ApacheDnsResolver
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
                setMaxConnPerRoute(1)
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
                if (isAtLeastQ) {
                    val resolver = DnsResolver.getInstance()
                    val executor = Dispatchers.IO.asExecutor()
                    setDnsResolver(
                        object : ApacheDnsResolver {
                            override fun resolve(host: String) = query(host, resolver, executor).toTypedArray()
                            override fun resolveCanonicalHostname(host: String) = unreachable()
                        },
                    )
                }
            }.build(),
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun query(host: String, resolver: DnsResolver, executor: Executor) = runBlocking {
    suspend fun doQuery(type: Int) = suspendCoroutine { cont ->
        resolver.query(
            null,
            host,
            type,
            DnsResolver.FLAG_EMPTY,
            executor,
            null,
            object : DnsResolver.Callback<List<InetAddress>> {
                override fun onAnswer(result: List<InetAddress>, r: Int) {
                    cont.resume(result)
                }
                override fun onError(e: DnsResolver.DnsException) {
                    cont.resumeWithException(e)
                }
            },
        )
    }

    try {
        parZip(
            { doQuery(DnsResolver.TYPE_A) },
            { doQuery(DnsResolver.TYPE_AAAA) },
            { a, b -> (a + b).shuffled() },
        )
    } catch (e: Exception) {
        throw UnknownHostException(e.message).apply { initCause(e) }
    }
}
