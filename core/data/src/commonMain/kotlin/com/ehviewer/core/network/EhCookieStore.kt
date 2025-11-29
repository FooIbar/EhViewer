package com.ehviewer.core.network

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url

object EhCookieStore : CookiesStorage {
    private val manager = getCookieManager()
    private val urlE = URLBuilder(URLProtocol.HTTPS, "e-hentai.org").build()
    private val urlEx = URLBuilder(URLProtocol.HTTPS, "exhentai.org").build()

    fun removeAllCookies() = manager.removeAllCookies()

    fun hasSignedIn(): Boolean = manager.getCookies(urlE)?.run {
        containsKey(KEY_IPB_MEMBER_ID) && containsKey(KEY_IPB_PASS_HASH)
    } == true

    const val KEY_IPB_MEMBER_ID = "ipb_member_id"
    const val KEY_IPB_PASS_HASH = "ipb_pass_hash"
    const val KEY_IGNEOUS = "igneous"
    private const val KEY_HATH_PERKS = "hath_perks"
    private const val KEY_CONTENT_WARNING = "nw"
    private const val CONTENT_WARNING_NOT_SHOW = "1"
    private const val KEY_UTMP_NAME = "__utmp"
    private val sTipsCookie = Cookie(
        name = KEY_CONTENT_WARNING,
        value = CONTENT_WARNING_NOT_SHOW,
    )

    fun clearIgneous() {
        manager.setCookie(
            urlEx,
            Cookie(KEY_IGNEOUS, "", maxAge = 0, domain = urlEx.host, path = "/"),
        )
    }

    fun getUserId() = manager.getCookies(urlE)?.get(KEY_IPB_MEMBER_ID)

    fun getHathPerks() = manager.getCookies(urlE)?.get(KEY_HATH_PERKS)?.substringBefore('-')

    fun getIdentityCookies(): List<Pair<String, String?>> {
        val eCookies = manager.getCookies(urlE)
        val exCookies = manager.getCookies(urlEx)
        val ipbMemberId = eCookies?.get(KEY_IPB_MEMBER_ID)
        val ipbPassHash = eCookies?.get(KEY_IPB_PASS_HASH)
        val igneous = exCookies?.get(KEY_IGNEOUS)
        return listOf(
            KEY_IPB_MEMBER_ID to ipbMemberId,
            KEY_IPB_PASS_HASH to ipbPassHash,
            KEY_IGNEOUS to igneous,
        )
    }

    fun isCloudflareBypassed() = manager.getCookies(urlE)?.containsKey("cf_clearance") == true

    fun flush() = manager.flush()

    // See https://github.com/Ehviewer-Overhauled/Ehviewer/issues/873
    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (cookie.name != KEY_UTMP_NAME) {
            manager.setCookie(requestUrl, cookie)
        }
    }

    override fun close() = Unit

    override suspend fun get(requestUrl: Url): List<Cookie> {
        val checkTips = requestUrl.host == urlE.host
        return manager.getCookies(requestUrl)?.mapTo(mutableListOf()) {
            Cookie(it.key, it.value)
        }?.apply {
            if (checkTips) {
                add(sTipsCookie)
            }
        } ?: if (checkTips) listOf(sTipsCookie) else emptyList()
    }
}
