package com.ehviewer.core.network

import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.parseClientCookiesHeader
import io.ktor.http.renderSetCookieHeader

class AndroidCookieManager : CookieManager {
    private val manager = android.webkit.CookieManager.getInstance()
    override fun getCookies(url: Url) = manager.getCookie(url.toString())?.let { parseClientCookiesHeader(it) }
    override fun setCookie(url: Url, cookie: Cookie) = manager.setCookie(url.toString(), renderSetCookieHeader(cookie))
    override fun removeAllCookies() = manager.removeAllCookies(null)
    override fun flush() = manager.flush()
}

actual fun getCookieManager(): CookieManager = AndroidCookieManager()
