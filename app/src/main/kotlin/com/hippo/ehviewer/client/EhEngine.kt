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

import arrow.core.Either
import arrow.core.left
import arrow.core.partially2
import arrow.core.right
import arrow.fx.coroutines.parMap
import arrow.fx.coroutines.parZip
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.exception.InsufficientFundsException
import com.hippo.ehviewer.client.exception.NotLoggedInException
import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.ehviewer.client.parser.ArchiveParser
import com.hippo.ehviewer.client.parser.EventPaneParser
import com.hippo.ehviewer.client.parser.FavParserResult
import com.hippo.ehviewer.client.parser.FavoritesParser
import com.hippo.ehviewer.client.parser.ForumsParser
import com.hippo.ehviewer.client.parser.GalleryApiParser
import com.hippo.ehviewer.client.parser.GalleryDetailParser
import com.hippo.ehviewer.client.parser.GalleryListParser
import com.hippo.ehviewer.client.parser.GalleryNotAvailableParser
import com.hippo.ehviewer.client.parser.GalleryPageParser
import com.hippo.ehviewer.client.parser.GalleryTokenApiParser
import com.hippo.ehviewer.client.parser.HomeParser
import com.hippo.ehviewer.client.parser.ProfileParser
import com.hippo.ehviewer.client.parser.RateGalleryResult
import com.hippo.ehviewer.client.parser.SignInParser
import com.hippo.ehviewer.client.parser.TorrentParser
import com.hippo.ehviewer.client.parser.TorrentResult
import com.hippo.ehviewer.client.parser.UserConfigParser
import com.hippo.ehviewer.client.parser.VoteCommentResult
import com.hippo.ehviewer.client.parser.VoteTagParser
import com.hippo.ehviewer.dailycheck.showEventNotification
import com.hippo.ehviewer.dailycheck.today
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.StatusCodeException
import com.hippo.ehviewer.util.bodyAsUtf8Text
import eu.kanade.tachiyomi.util.system.logcat
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.pool.DirectByteBufferPool
import io.ktor.utils.io.pool.useInstance
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.put
import moe.tarsin.coroutines.removeAllSuspend
import moe.tarsin.coroutines.runSuspendCatching
import okio.buffer
import okio.sink
import org.jsoup.Jsoup
import splitties.init.appCtx

// https://ehwiki.org/wiki/API#Basics
private const val MAX_REQUEST_SIZE = 25
private const val MAX_SEQUENTIAL_REQUESTS = 5
private const val REQUEST_INTERVAL = 5000L

fun Either<String, ByteBuffer>.saveParseError(e: Throwable) {
    val dir = AppConfig.externalParseErrorDir ?: return
    val file = File(dir, ReadableTime.getFilenamableTime() + ".txt")
    file.sink().buffer().use { sink ->
        with(sink) {
            writeUtf8(e.message + "\n")
            onLeft { writeUtf8(it) }
            onRight { write(it) }
        }
    }
}

fun rethrowExactly(code: Int, body: Either<String, ByteBuffer>, e: Throwable): Nothing {
    // Don't translate coroutine cancellation
    if (e is CancellationException) throw e

    // Check sad panda (without panda)
    val empty = body.fold(
        { it.isEmpty() },
        { !it.hasRemaining() },
    )
    if (empty) {
        if (EhUtils.isExHentai) {
            throw EhException("Sad Panda\n(without panda)")
        } else {
            throw EhException("IP banned")
        }
    }

    // Check Gallery Not Available
    body.onLeft {
        if ("Gallery Not Available - " in it) {
            val error = GalleryNotAvailableParser.parse(it)
            if (!error.isNullOrBlank()) {
                throw EhException(error)
            }
        }
    }

    // Check bad response code
    if (code >= 400) {
        throw StatusCodeException(code)
    }

    if (e is ParseException || e is SerializationException) {
        body.onLeft { if ("<" !in it) throw EhException(it) }
        when (val message = e.cause?.message) {
            "No hits found!" -> throw EhException(R.string.gallery_list_empty_hit)
            "No watched tags!" -> throw EhException(R.string.gallery_list_empty_hit_subscription)
            "Not logged in!" -> throw NotLoggedInException()
            is String -> if (message.startsWith("Your IP address")) throw EhException(message)
        }
        if (Settings.saveParseErrorBody) body.saveParseError(e)
        throw EhException(appCtx.getString(R.string.error_parse_error), e)
    }

    // We can't translate it, rethrow it anyway
    throw e
}

val httpContentPool = DirectByteBufferPool(8, 0x80000)

suspend inline fun <T> HttpStatement.fetchUsingAsText(crossinline block: suspend String.() -> T) = executeSafely { response ->
    val body = response.bodyAsUtf8Text()
    runSuspendCatching {
        block(body)
    }.onFailure {
        rethrowExactly(response.status.value, body.left(), it)
    }.getOrThrow()
}

suspend inline fun <T> HttpStatement.fetchUsingAsByteBuffer(crossinline block: suspend ByteBuffer.() -> T) = executeSafely { response ->
    httpContentPool.useInstance { buffer ->
        with(response.bodyAsChannel()) { while (!isClosedForRead) readAvailable(buffer) }
        buffer.flip()
        runSuspendCatching {
            block(buffer)
        }.onFailure {
            buffer.rewind()
            rethrowExactly(response.status.value, buffer.right(), it)
        }.getOrThrow()
    }
}

object EhEngine {
    suspend fun getOriginalImageUrl(url: String, referer: String?) = noRedirectEhRequest(url, referer).executeSafely { response ->
        val location = response.headers["Location"] ?: throw InsufficientFundsException()
        location.takeIf { "bounce_login" !in it } ?: throw NotLoggedInException()
    }

    suspend fun getTorrentList(url: String, gid: Long, token: String): TorrentResult {
        val referer = EhUrl.getGalleryDetailUrl(gid, token)
        return ehRequest(url, referer).fetchUsingAsByteBuffer(TorrentParser::parse)
    }

    suspend fun getArchiveList(url: String, gid: Long, token: String): ArchiveParser.Result {
        val funds = if (EhUtils.isExHentai) getFunds() else null
        return ehRequest(url, EhUrl.getGalleryDetailUrl(gid, token))
            .fetchUsingAsText(ArchiveParser::parse.partially2(funds))
    }

    suspend fun getImageLimits() = parZip(
        { ehRequest(EhUrl.URL_HOME).fetchUsingAsByteBuffer(HomeParser::parse) },
        { getFunds() },
        { limits, funds -> HomeParser.Result(limits, funds) },
    )

    private suspend fun getFunds() = ehRequest(EhUrl.URL_FUNDS).fetchUsingAsText(HomeParser::parseFunds)

    suspend fun getNews(parse: Boolean) = ehRequest(EhUrl.URL_NEWS, EhUrl.REFERER_E)
        .fetchUsingAsText { if (parse) EventPaneParser.parse(this) else null }

    suspend fun getProfile(): ProfileParser.Result {
        val url = ehRequest(EhUrl.URL_FORUMS).fetchUsingAsText(ForumsParser::parse)
        return ehRequest(url, EhUrl.URL_FORUMS).fetchUsingAsText(ProfileParser::parse)
    }

    suspend fun getUConfig(url: String = EhUrl.uConfigUrl) {
        runSuspendCatching {
            ehRequest(url).fetchUsingAsByteBuffer(UserConfigParser::parse)
        }.onFailure { throwable ->
            // It may get redirected when accessing ex for the first time
            if (url == EhUrl.URL_UCONFIG_EX) {
                logcat(throwable)
                ehRequest(url).fetchUsingAsByteBuffer(UserConfigParser::parse)
            } else {
                throw throwable
            }
        }
    }

    suspend fun getGalleryPage(url: String, gid: Long, token: String): GalleryPageParser.Result {
        val referer = EhUrl.getGalleryDetailUrl(gid, token)
        return ehRequest(url, referer).fetchUsingAsText(GalleryPageParser::parse)
    }

    suspend fun getGalleryList(url: String) = ehRequest(url, EhUrl.referer).fetchUsingAsByteBuffer(GalleryListParser::parse)
        .apply { galleryInfoList.fillInfo(url, true) }
        .takeUnless { it.galleryInfoList.isEmpty() } ?: GalleryListParser.emptyResult

    suspend fun getGalleryDetail(url: String) = ehRequest(url, EhUrl.referer).fetchUsingAsText {
        val eventPane = EventPaneParser.parse(this)
        if (eventPane != null) {
            Settings.lastDawnDays = today
            showEventNotification(eventPane)
        }
        GalleryDetailParser.parse(this)
    }

    suspend fun getPreviewList(url: String) = ehRequest(url, EhUrl.referer).fetchUsingAsText {
        GalleryDetailParser.parsePreviewList(this)
    }

    suspend fun getFavorites(url: String) = ehRequest(url, EhUrl.referer).fetchUsingAsByteBuffer(FavoritesParser::parse)
        .apply { galleryInfoList.fillInfo(url) }

    suspend fun signIn(username: String, password: String): String {
        val referer = "https://forums.e-hentai.org/index.php?act=Login&CODE=00"
        val url = EhUrl.API_SIGN_IN
        val origin = "https://forums.e-hentai.org"
        return ehRequest(url, referer, origin) {
            formBody {
                append("referer", referer)
                append("b", "")
                append("bt", "")
                append("UserName", username)
                append("PassWord", password)
                append("CookieDate", "1")
            }
        }.fetchUsingAsText(SignInParser::parse)
    }

    suspend fun commentGallery(url: String, comment: String, id: Long = -1) = ehRequest(url, url, EhUrl.origin) {
        formBody {
            if (id == -1L) {
                append("commenttext_new", comment)
            } else {
                append("commenttext_edit", comment)
                append("edit_comment", id.toString())
            }
        }
    }.executeSafely { response ->
        // Ktor does not handle POST redirect, we need to do it manually
        // https://youtrack.jetbrains.com/issue/KTOR-478
        val location = response.headers["Location"] ?: url
        ehRequest(location, url).fetchUsingAsText {
            val document = Jsoup.parse(this)
            val elements = document.select("#chd + p")
            if (elements.size > 0) {
                throw EhException(elements[0].text())
            }
            GalleryDetailParser.parseComments(document)
        }
    }

    suspend fun modifyFavorites(gid: Long, token: String, dstCat: Int = -1, note: String = "") {
        val catStr: String = when (dstCat) {
            -1 -> "favdel"
            in 0..9 -> dstCat.toString()
            else -> throw EhException("Invalid dstCat: $dstCat")
        }
        val url = EhUrl.getAddFavorites(gid, token)
        ehRequest(url, url, EhUrl.origin) {
            formBody {
                append("favcat", catStr)
                append("favnote", note)
                // apply=Add+to+Favorites is not necessary, just use apply=Apply+Changes all the time
                append("apply", "Apply Changes")
                append("update", "1")
            }
        }.executeSafely { }
    }

    suspend fun getFavoriteNote(gid: Long, token: String) =
        ehRequest(EhUrl.getAddFavorites(gid, token), EhUrl.getGalleryDetailUrl(gid, token))
            .fetchUsingAsText(FavoritesParser::parseNote)

    suspend fun downloadArchive(gid: Long, token: String, or: String, res: String, isHAtH: Boolean): String? {
        val url = EhUrl.getDownloadArchive(gid, token, or)
        val referer = EhUrl.getGalleryDetailUrl(gid, token)
        val request = ehRequest(url, referer, EhUrl.origin) {
            formBody {
                if (isHAtH) {
                    append("hathdl_xres", res)
                } else {
                    append("dltype", res)
                    if (res == "org") {
                        append("dlcheck", "Download Original Archive")
                    } else {
                        append("dlcheck", "Download Resample Archive")
                    }
                }
            }
        }
        var result = request.fetchUsingAsText(ArchiveParser::parseArchiveUrl)
        if (!isHAtH) {
            if (result == null) {
                // Wait for the server to prepare archives
                delay(1000)
                result = request.fetchUsingAsText(ArchiveParser::parseArchiveUrl)
                if (result == null) throw EhException("Archive unavailable")
            }
            return result
        }
        return null
    }

    suspend fun resetImageLimits() = ehRequest(EhUrl.URL_HOME) {
        formBody {
            append("reset_imagelimit", "Reset Limit")
        }
    }.fetchUsingAsText(HomeParser::parseResetLimits)

    suspend fun modifyFavorites(gidArray: LongArray, srcCat: Int, dstCat: Int): FavParserResult {
        val url = ehUrl(EhUrl.FAV_PATH) {
            if (FavListUrlBuilder.isValidFavCat(srcCat)) addQueryParameter("favcat", srcCat.toString())
        }.buildString()
        val catStr: String = when (dstCat) {
            -1 -> "delete"
            in 0..9 -> "fav$dstCat"
            else -> throw EhException("Invalid dstCat: $dstCat")
        }
        return ehRequest(url, url, EhUrl.origin) {
            formBody {
                append("ddact", catStr)
                gidArray.forEach { append("modifygids[]", it.toString()) }
            }
        }.fetchUsingAsByteBuffer(FavoritesParser::parse).apply { galleryInfoList.fillInfo(url) }
    }

    suspend fun getGalleryPageApi(gid: Long, index: Int, pToken: String, showKey: String?, previousPToken: String?): GalleryPageParser.Result {
        val referer = if (index > 0 && previousPToken != null) EhUrl.getPageUrl(gid, index - 1, previousPToken) else null
        return ehRequest(EhUrl.apiUrl, referer, EhUrl.origin) {
            jsonBody {
                put("method", "showpage")
                put("gid", gid)
                put("page", index + 1)
                put("imgkey", pToken)
                put("showkey", showKey)
            }
        }.fetchUsingAsText { GalleryPageParser.parse(filterNot { it == '\\' }) }
    }

    suspend fun rateGallery(apiUid: Long, apiKey: String?, gid: Long, token: String, rating: Float): RateGalleryResult = ehRequest(EhUrl.apiUrl, EhUrl.getGalleryDetailUrl(gid, token), EhUrl.origin) {
        jsonBody {
            put("method", "rategallery")
            put("apiuid", apiUid)
            put("apikey", requireNotNull(apiKey))
            put("gid", gid)
            put("token", token)
            put("rating", ceil((rating * 2).toDouble()).toInt())
        }
    }.fetchUsingAsText(String::parseAs)

    suspend fun fillGalleryListByApi(galleryInfoList: List<GalleryInfo>, referer: String? = null) =
        galleryInfoList.chunked(MAX_REQUEST_SIZE).chunked(MAX_SEQUENTIAL_REQUESTS).forEachIndexed { index, chunk ->
            if (index != 0) {
                delay(REQUEST_INTERVAL)
            }
            chunk.parMap {
                ehRequest(EhUrl.apiUrl, referer, EhUrl.origin) {
                    jsonBody {
                        put("method", "gdata")
                        array("gidlist") {
                            it.forEach {
                                addJsonArray {
                                    add(it.gid)
                                    add(it.token)
                                }
                            }
                        }
                        put("namespace", 1)
                    }
                }.fetchUsingAsText { GalleryApiParser.parse(this, it) }
            }
        }

    suspend fun voteComment(apiUid: Long, apiKey: String?, gid: Long, token: String, commentId: Long, commentVote: Int): VoteCommentResult =
        ehRequest(EhUrl.apiUrl, EhUrl.referer, EhUrl.origin) {
            jsonBody {
                put("method", "votecomment")
                put("apiuid", apiUid)
                put("apikey", requireNotNull(apiKey))
                put("gid", gid)
                put("token", token)
                put("comment_id", commentId)
                put("comment_vote", commentVote)
            }
        }.fetchUsingAsText(String::parseAs)

    suspend fun voteTag(apiUid: Long, apiKey: String?, gid: Long, token: String, tags: String, vote: Int) =
        ehRequest(EhUrl.apiUrl, EhUrl.referer, EhUrl.origin) {
            jsonBody {
                put("method", "taggallery")
                put("apiuid", apiUid)
                put("apikey", requireNotNull(apiKey))
                put("gid", gid)
                put("token", token)
                put("tags", tags)
                put("vote", vote)
            }
        }.fetchUsingAsText(VoteTagParser::parse)

    suspend fun getGalleryToken(gid: Long, gtoken: String, page: Int) = ehRequest(EhUrl.apiUrl, EhUrl.referer, EhUrl.origin) {
        jsonBody {
            put("method", "gtoken")
            array("pagelist") {
                addJsonArray {
                    add(gid)
                    add(gtoken)
                    add(page + 1)
                }
            }
        }
    }.fetchUsingAsText(GalleryTokenApiParser::parse)

    private suspend fun MutableList<BaseGalleryInfo>.fillInfo(url: String, filter: Boolean = false) = with(EhFilter) {
        if (filter) removeAllSuspend { filterTitle(it) || filterUploader(it) }
        val hasTags = any { !it.simpleTags.isNullOrEmpty() }
        val hasPages = any { it.pages != 0 }
        val hasRated = any { it.rated }
        val needApi = filter && needTags() && !hasTags || Settings.showGalleryPages.value && !hasPages || hasRated
        if (needApi) fillGalleryListByApi(this@fillInfo, url)
        if (filter) removeAllSuspend { filterUploader(it) || filterTag(it) || filterTagNamespace(it) }
        forEach {
            if (it.favoriteSlot == GalleryInfo.NOT_FAVORITED && EhDB.containLocalFavorites(it.gid)) {
                it.favoriteSlot = GalleryInfo.LOCAL_FAVORITED
            }
            if (!needApi) it.generateSLang()
        }
    }

    suspend fun addFavorites(galleryList: List<Pair<Long, String>>, dstCat: Int) {
        galleryList.forEach { (gid, token) ->
            // https://github.com/FooIbar/EhViewer/issues/1190
            // Workaround for duplicate items when sorting by favorited time
            val timeTaken = measureTimeMillis {
                modifyFavorites(gid, token, dstCat)
            }
            delay(1000 - timeTaken)
        }
    }
}
