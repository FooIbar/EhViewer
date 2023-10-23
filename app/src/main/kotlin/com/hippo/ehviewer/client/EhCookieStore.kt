/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.client

import android.webkit.CookieManager
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.util.fastMapNotNull
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

object EhCookieStore : CookieJar {
    private val manager = CookieManager.getInstance()
    fun signOut() = manager.removeAllCookies(null)
    fun contains(url: HttpUrl, name: String) = get(url).fastAny { it.name == name }

    fun get(url: HttpUrl): List<Cookie> {
        val cookies = manager.getCookie(url.toString())

        return if (!cookies.isNullOrEmpty()) {
            cookies.split(";").fastMapNotNull { setCookie ->
                Cookie.parse(url, setCookie)?.takeUnless { it.name == KEY_UTMP_NAME }
            }
        } else {
            emptyList()
        }
    }

    fun hasSignedIn(): Boolean {
        val url = EhUrl.HOST_E.toHttpUrl()
        return contains(url, KEY_IPB_MEMBER_ID) && contains(url, KEY_IPB_PASS_HASH)
    }

    const val KEY_IPB_MEMBER_ID = "ipb_member_id"
    const val KEY_IPB_PASS_HASH = "ipb_pass_hash"
    const val KEY_IGNEOUS = "igneous"
    private const val KEY_STAR = "star"
    private const val KEY_CONTENT_WARNING = "nw"
    private const val CONTENT_WARNING_NOT_SHOW = "1"
    private const val KEY_UTMP_NAME = "__utmp"
    private val sTipsCookie = Cookie.Builder().apply {
        name(KEY_CONTENT_WARNING)
        value(CONTENT_WARNING_NOT_SHOW)
        domain(EhUrl.DOMAIN_E)
        path("/")
        expiresAt(Long.MAX_VALUE)
    }.build()

    fun copyNecessaryCookies() {
        val cookies = get(EhUrl.HOST_E.toHttpUrl())
        cookies.fastForEach {
            if (it.name == KEY_STAR || it.name == KEY_IPB_MEMBER_ID || it.name == KEY_IPB_PASS_HASH) {
                manager.setCookie(EhUrl.HOST_EX, it.toString())
            }
        }
    }

    fun deleteCookie(url: HttpUrl, name: String) {
        manager.setCookie(url.toString(), "$name=;Max-Age=0")
    }

    fun addCookie(cookie: Cookie) {
        manager.setCookie(if (EhUrl.DOMAIN_E == cookie.domain) EhUrl.HOST_E else EhUrl.HOST_EX, cookie.toString())
    }

    fun flush() = manager.flush()

    fun getCookieHeader(url: HttpUrl): String {
        return loadForRequest(url).fastJoinToString("; ") {
            "${it.name}=${it.value}"
        }
    }

    // See https://github.com/Ehviewer-Overhauled/Ehviewer/issues/873
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = cookies.fastForEach {
        if (it.name != KEY_UTMP_NAME) {
            manager.setCookie(url.toString(), it.toString())
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val checkTips = EhUrl.DOMAIN_E in url.host
        return get(url).run {
            if (checkTips) {
                fastFilter { it.name != KEY_CONTENT_WARNING }.toMutableList().apply { add(sTipsCookie) }
            } else {
                this
            }
        }
    }
}
