package com.hippo.ehviewer.ktor

import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.client.EhCookieStore
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.SaveBodyPlugin
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.util.appendIfNameAbsent

const val CHROME_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${BuildConfig.CHROME_VERSION}.0.0.0 Safari/537.36"
private const val CHROME_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
private const val CHROME_ACCEPT_LANGUAGE = "en-US,en;q=0.9"

fun <T : HttpClientEngineConfig> HttpClientConfig<T>.configureCommon(redirect: Boolean = true) = apply {
    install(HttpCookies) {
        storage = EhCookieStore
    }
    install(HttpTimeout) {
        reset()
        requestTimeoutMillis = 10_000
    }
    install(UserAgent) {
        agent = CHROME_USER_AGENT
    }
    defaultRequest {
        headers.appendIfNameAbsent(HttpHeaders.Accept, CHROME_ACCEPT)
        header(HttpHeaders.AcceptLanguage, CHROME_ACCEPT_LANGUAGE)
    }
    followRedirects = redirect
    install(SaveBodyPlugin) {
        disabled = true
    }
}

fun HttpTimeoutConfig.reset() = apply {
    requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
    connectTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
    socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
}
