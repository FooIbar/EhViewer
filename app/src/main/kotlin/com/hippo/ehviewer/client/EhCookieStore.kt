package com.hippo.ehviewer.client

import android.webkit.CookieManager
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastJoinToString
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Url
import io.ktor.http.parseClientCookiesHeader
import io.ktor.http.renderSetCookieHeader
import okhttp3.HttpUrl

typealias KtorCookie = io.ktor.http.Cookie

object EhCookieStore : CookiesStorage {
    private val manager = CookieManager.getInstance()
    fun signOut() = manager.removeAllCookies(null)
    fun contains(url: String, name: String) = load(Url(url)).fastAny { it.name == name }

    fun hasSignedIn(): Boolean {
        val url = EhUrl.HOST_E
        return contains(url, KEY_IPB_MEMBER_ID) && contains(url, KEY_IPB_PASS_HASH)
    }

    const val KEY_IPB_MEMBER_ID = "ipb_member_id"
    const val KEY_IPB_PASS_HASH = "ipb_pass_hash"
    const val KEY_IGNEOUS = "igneous"
    private const val KEY_STAR = "star"
    private const val KEY_CONTENT_WARNING = "nw"
    private const val CONTENT_WARNING_NOT_SHOW = "1"
    private const val KEY_UTMP_NAME = "__utmp"
    private val sTipsCookie = KtorCookie(
        name = KEY_CONTENT_WARNING,
        value = CONTENT_WARNING_NOT_SHOW,
    )

    fun copyNecessaryCookies() {
        val cookies = load(Url(EhUrl.HOST_E))
        cookies.fastForEach {
            if (it.name == KEY_STAR || it.name == KEY_IPB_MEMBER_ID || it.name == KEY_IPB_PASS_HASH) {
                manager.setCookie(EhUrl.HOST_EX, it.toString())
            }
        }
    }

    fun deleteCookie(url: HttpUrl, name: String) {
        manager.setCookie(url.toString(), "$name=;Max-Age=0")
    }

    fun addCookie(k: String, v: String, domain: String) {
        val cookie = KtorCookie(name = k, value = v, domain = domain)
        val url = if (EhUrl.DOMAIN_E == cookie.domain) EhUrl.HOST_E else EhUrl.HOST_EX
        manager.setCookie(url, renderSetCookieHeader(cookie))
    }

    fun flush() = manager.flush()

    fun getCookieHeader(url: String): String {
        return load(Url(url)).fastJoinToString("; ") {
            "${it.name}=${it.value}"
        }
    }

    // See https://github.com/Ehviewer-Overhauled/Ehviewer/issues/873
    override suspend fun addCookie(requestUrl: Url, cookie: KtorCookie) {
        if (cookie.name != KEY_UTMP_NAME) {
            manager.setCookie(requestUrl.toString(), renderSetCookieHeader(cookie))
        }
    }

    override fun close() = Unit

    fun load(url: Url): List<KtorCookie> {
        val cookies = manager.getCookie(url.toString()) ?: return emptyList()
        val checkTips = EhUrl.DOMAIN_E in url.host
        return parseClientCookiesHeader(cookies)
            .map { KtorCookie(it.key, it.value) }
            .filterTo(mutableListOf()) { it.name != KEY_UTMP_NAME }.apply {
                if (checkTips) {
                    removeAll { it.name == KEY_CONTENT_WARNING }
                    add(sTipsCookie)
                }
            }
    }

    override suspend fun get(requestUrl: Url) = load(requestUrl)
}
