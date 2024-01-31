package com.hippo.ehviewer.client

import android.webkit.CookieManager
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastJoinToString
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.parseClientCookiesHeader
import io.ktor.http.renderCookieHeader
import io.ktor.http.renderSetCookieHeader

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
    private const val KEY_CONTENT_WARNING = "nw"
    private const val CONTENT_WARNING_NOT_SHOW = "1"
    private const val KEY_UTMP_NAME = "__utmp"
    private val sTipsCookie = Cookie(
        name = KEY_CONTENT_WARNING,
        value = CONTENT_WARNING_NOT_SHOW,
    )

    fun clearIgneous() {
        manager.setCookie(
            EhUrl.HOST_EX,
            renderSetCookieHeader(KEY_IGNEOUS, "", maxAge = 0, domain = EhUrl.DOMAIN_EX, path = "/"),
        )
    }

    fun getIdentityCookies(): List<Pair<String, String?>> {
        val eCookies = getCookies(EhUrl.HOST_E)
        val exCookies = getCookies(EhUrl.HOST_EX)
        val ipbMemberId = eCookies?.get(KEY_IPB_MEMBER_ID)
        val ipbPassHash = eCookies?.get(KEY_IPB_PASS_HASH)
        val igneous = exCookies?.get(KEY_IGNEOUS)
        return listOf(
            KEY_IPB_MEMBER_ID to ipbMemberId,
            KEY_IPB_PASS_HASH to ipbPassHash,
            KEY_IGNEOUS to igneous,
        )
    }

    fun flush() = manager.flush()

    fun getCookieHeader(url: String): String {
        return load(Url(url)).fastJoinToString("; ") {
            renderCookieHeader(it)
        }
    }

    // See https://github.com/Ehviewer-Overhauled/Ehviewer/issues/873
    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (cookie.name != KEY_UTMP_NAME) {
            manager.setCookie(requestUrl.toString(), renderSetCookieHeader(cookie))
        }
    }

    override fun close() = Unit

    private fun getCookies(url: String) = manager.getCookie(url)?.let { parseClientCookiesHeader(it) }

    fun load(url: Url): List<Cookie> {
        val checkTips = EhUrl.DOMAIN_E in url.host
        return getCookies(url.toString())?.mapTo(mutableListOf()) {
            Cookie(it.key, it.value)
        }?.apply {
            if (checkTips) {
                add(sTipsCookie)
            }
        } ?: if (checkTips) listOf(sTipsCookie) else emptyList()
    }

    override suspend fun get(requestUrl: Url) = load(requestUrl)
}
