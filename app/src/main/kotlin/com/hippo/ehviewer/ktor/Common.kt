package com.hippo.ehviewer.ktor

import com.hippo.ehviewer.client.EhCookieStore
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies

fun <T : HttpClientEngineConfig> HttpClientConfig<T>.configureCommon() = apply {
    install(HttpCookies) {
        storage = EhCookieStore
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 10_000
    }
}
