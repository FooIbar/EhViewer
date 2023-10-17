package com.hippo.ehviewer.legacy

import arrow.fx.coroutines.release
import arrow.fx.coroutines.resource
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.dao.Cookie
import com.hippo.ehviewer.dao.CookiesDatabase
import splitties.arch.room.roomDb
import splitties.init.appCtx

private const val LEGACY_COOKIES_DB = "okhttp3-cookie.db"

private fun Cookie.toOkHttp3Cookie(): okhttp3.Cookie {
    return okhttp3.Cookie.Builder().apply {
        name(name)
        value(value)
        expiresAt(expiresAt)
        if (hostOnly) hostOnlyDomain(domain) else domain(domain)
        path(path)
        if (secure) secure()
        if (httpOnly) httpOnly()
    }.build()
}

suspend fun migrateCookies() {
    if (LEGACY_COOKIES_DB !in appCtx.databaseList()) return
    resource {
        roomDb<CookiesDatabase>(LEGACY_COOKIES_DB)
    } release {
        it.close()
        appCtx.deleteDatabase(LEGACY_COOKIES_DB)
    } use { database ->
        database.cookiesDao().list().forEach {
            EhCookieStore.addCookie(it.toOkHttp3Cookie())
        }
        EhCookieStore.flush()
    }
}
