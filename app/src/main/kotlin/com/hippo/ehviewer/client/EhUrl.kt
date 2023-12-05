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

import com.hippo.ehviewer.Settings
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol

object EhUrl {
    const val SITE_E = 0
    const val SITE_EX = 1
    const val DOMAIN_EX = "exhentai.org"
    const val DOMAIN_E = "e-hentai.org"
    const val HOST_EX = "https://$DOMAIN_EX/"
    const val API_EX = "https://s.exhentai.org/api.php"
    const val FAV_PATH = "favorites.php"
    const val WATCHED_PATH = "watched"
    const val URL_UCONFIG_EX = HOST_EX + "uconfig.php"
    const val URL_MY_TAGS_EX = HOST_EX + "mytags"
    const val HOST_E = "https://$DOMAIN_E/"
    const val API_E = "https://api.e-hentai.org/api.php"
    const val URL_UCONFIG_E = HOST_E + "uconfig.php"
    const val URL_MY_TAGS_E = HOST_E + "mytags"
    const val API_SIGN_IN = "https://forums.e-hentai.org/index.php?act=Login&CODE=01"
    const val URL_POPULAR_E = "https://e-hentai.org/popular"
    const val URL_POPULAR_EX = "https://exhentai.org/popular"
    const val URL_IMAGE_SEARCH_E = "https://upload.e-hentai.org/image_lookup.php"
    const val URL_IMAGE_SEARCH_EX = "https://exhentai.org/upload/image_lookup.php"
    const val URL_SIGN_IN = "https://forums.e-hentai.org/index.php?act=Login"
    const val URL_REGISTER = "https://forums.e-hentai.org/index.php?act=Reg&CODE=00"
    const val URL_FORUMS = "https://forums.e-hentai.org/"
    const val URL_FUNDS = HOST_E + "exchange.php?t=gp"
    const val URL_HOME = HOST_E + "home.php"
    const val URL_NEWS = HOST_E + "news.php"
    const val REFERER_EX = "https://$DOMAIN_EX"
    const val ORIGIN_EX = REFERER_EX
    const val REFERER_E = "https://$DOMAIN_E"
    const val ORIGIN_E = REFERER_E

    val host: String
        get() = when (Settings.gallerySite) {
            SITE_E -> HOST_E
            SITE_EX -> HOST_EX
            else -> HOST_E
        }

    val domain: String
        get() = when (Settings.gallerySite) {
            SITE_E -> DOMAIN_E
            SITE_EX -> DOMAIN_EX
            else -> HOST_E
        }

    val apiUrl: String
        get() = if (Settings.forceEhThumb) {
            API_E
        } else {
            when (Settings.gallerySite) {
                SITE_E -> API_E
                SITE_EX -> API_EX
                else -> API_E
            }
        }

    val referer: String
        get() = when (Settings.gallerySite) {
            SITE_E -> REFERER_E
            SITE_EX -> REFERER_EX
            else -> REFERER_E
        }

    val origin: String
        get() = when (Settings.gallerySite) {
            SITE_E -> ORIGIN_E
            SITE_EX -> ORIGIN_EX
            else -> ORIGIN_E
        }

    val uConfigUrl: String
        get() = when (Settings.gallerySite) {
            SITE_E -> URL_UCONFIG_E
            SITE_EX -> URL_UCONFIG_EX
            else -> URL_UCONFIG_E
        }

    val myTagsUrl: String
        get() = when (Settings.gallerySite) {
            SITE_E -> URL_MY_TAGS_E
            SITE_EX -> URL_MY_TAGS_EX
            else -> URL_MY_TAGS_E
        }

    val popularUrl: String
        get() = when (Settings.gallerySite) {
            SITE_E -> URL_POPULAR_E
            SITE_EX -> URL_POPULAR_EX
            else -> URL_POPULAR_E
        }

    val imageSearchUrl: String
        get() = when (Settings.gallerySite) {
            SITE_E -> URL_IMAGE_SEARCH_E
            SITE_EX -> URL_IMAGE_SEARCH_EX
            else -> URL_IMAGE_SEARCH_E
        }

    fun getGalleryDetailUrl(gid: Long, token: String?): String {
        return getGalleryDetailUrl(gid, token, 0, false)
    }

    fun getGalleryDetailUrl(gid: Long, token: String?, index: Int, allComment: Boolean) = ehUrl(listOf("g", gid.toString(), token.orEmpty())) {
        if (index != 0) addQueryParameter("p", "$index")
        if (allComment) addQueryParameter("hc", "1")
    }.buildString()

    fun getGalleryMultiPageViewerUrl(gid: Long, token: String) = ehUrl(listOf("mpv", gid.toString(), token)).buildString()

    fun getPageUrl(gid: Long, index: Int, pToken: String) = host + "s/" + pToken + '/' + gid + '-' + (index + 1)

    fun getAddFavorites(gid: Long, token: String?) = host + "gallerypopups.php?gid=" + gid + "&t=" + token + "&act=addfav"

    fun getDownloadArchive(gid: Long, token: String?, or: String) = host + "archiver.php?gid=" + gid + "&token=" + token + "&or=" + or

    fun getTagDefinitionUrl(tag: String) = "https://ehwiki.org/wiki/" + tag.replace(' ', '_')
}

inline fun ehUrl(path: String?, builder: ParametersBuilder.() -> Unit) = ehUrl(
    path = if (path.isNullOrBlank()) emptyList() else listOf(path),
    parameters = Parameters.build(builder),
)

inline fun ehUrl(path: List<String>, builder: ParametersBuilder.() -> Unit) = ehUrl(
    path = path,
    parameters = Parameters.build(builder),
)

fun ehUrl(path: List<String>, parameters: Parameters = Parameters.Empty) = URLBuilder(
    protocol = URLProtocol.HTTPS,
    host = EhUrl.domain,
    pathSegments = path,
    parameters = parameters,
)

fun ParametersBuilder.addQueryParameterIfNotBlank(name: String, value: String?) = apply {
    if (!value.isNullOrBlank()) append(name, value)
}

fun ParametersBuilder.addQueryParameter(name: String, value: String) = append(name, value)
