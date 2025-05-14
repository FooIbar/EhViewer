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

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import arrow.core.memoize
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.parser.Archive
import com.hippo.ehviewer.spider.SpiderDen
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.addTextToClipboard
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.from
import com.materialkolor.ktx.toColor
import eu.kanade.tachiyomi.util.lang.withUIContext
import splitties.systemservices.downloadManager

object EhUtils {
    const val NONE = -1 // Use it for homepage
    const val MISC = 0x1
    const val DOUJINSHI = 0x2
    const val MANGA = 0x4
    const val ARTIST_CG = 0x8
    const val GAME_CG = 0x10
    const val IMAGE_SET = 0x20
    const val COSPLAY = 0x40
    const val ASIAN_PORN = 0x80
    const val NON_H = 0x100
    const val WESTERN = 0x200
    const val ALL_CATEGORY = 0x3ff
    const val PRIVATE = 0x400
    const val UNKNOWN = 0x800

    // https://youtrack.jetbrains.com/issue/KT-4749
    private const val BG_COLOR_DOUJINSHI = 0xfff44336u
    private const val BG_COLOR_MANGA = 0xffff9800u
    private const val BG_COLOR_ARTIST_CG = 0xfffbc02du
    private const val BG_COLOR_GAME_CG = 0xff4caf50u
    private const val BG_COLOR_WESTERN = 0xff8bc34au
    private const val BG_COLOR_NON_H = 0xff2196f3u
    private const val BG_COLOR_IMAGE_SET = 0xff3f51b5u
    private const val BG_COLOR_COSPLAY = 0xff9c27b0u
    private const val BG_COLOR_ASIAN_PORN = 0xff9575cdu
    private const val BG_COLOR_MISC = 0xfff06292u
    private const val BG_COLOR_UNKNOWN = 0xff000000u

    // Remove [XXX], (XXX), {XXX}, ~XXX~ stuff
    private val PATTERN_TITLE_PREFIX = Regex(
        """^(?:\([^)]*\)|\[[^]]*]|\{[^}]*\}|~[^~]*~|\s+)*""",
    )

    // Remove [XXX], (XXX), {XXX}, ~XXX~ stuff and something like ch. 1-23
    private val PATTERN_TITLE_SUFFIX = Regex(
        """(?:\s+ch.[\s\d-]+)?(?:\([^)]*\)|\[[^]]*]|\{[^}]*\}|~[^~]*~|\s+)*$""",
        RegexOption.IGNORE_CASE,
    )

    private val CATEGORY_VALUES = hashMapOf(
        MISC to arrayOf("misc"),
        DOUJINSHI to arrayOf("doujinshi"),
        MANGA to arrayOf("manga"),
        ARTIST_CG to arrayOf("artistcg", "Artist CG Sets", "Artist CG"),
        GAME_CG to arrayOf("gamecg", "Game CG Sets", "Game CG"),
        IMAGE_SET to arrayOf("imageset", "Image Sets", "Image Set"),
        COSPLAY to arrayOf("cosplay"),
        ASIAN_PORN to arrayOf("asianporn", "Asian Porn"),
        NON_H to arrayOf("non-h"),
        WESTERN to arrayOf("western"),
        PRIVATE to arrayOf("private"),
        UNKNOWN to arrayOf("unknown"),
    )
    private val CATEGORY_STRINGS = CATEGORY_VALUES.entries.map { (k, v) -> v to k }

    val isExHentai: Boolean
        get() = Settings.gallerySite.value == EhUrl.SITE_EX

    val isMpvAvailable
        get() = EhCookieStore.getHathPerks()?.contains('q') == true

    fun getCategory(type: String?): Int {
        for (entry in CATEGORY_STRINGS) {
            for (str in entry.first) {
                if (str.equals(type, ignoreCase = true)) {
                    return entry.second
                }
            }
        }
        return UNKNOWN
    }

    fun getCategory(type: Int): String = CATEGORY_VALUES.getOrDefault(type, CATEGORY_VALUES[UNKNOWN])!![0]

    fun invCategory(category: Int): Int = category.inv() and ALL_CATEGORY

    val mergeColor = { primaryContainer: Color, src: Color ->
        val fromHct = Hct.from(src)
        val toHct = Hct.from(primaryContainer)
        Hct.from(fromHct.hue, toHct.chroma, toHct.tone).toColor()
    }.memoize()

    @Stable
    @ReadOnlyComposable
    @Composable
    fun getCategoryColor(category: Int): Color {
        val primary = Color(
            when (category) {
                DOUJINSHI -> BG_COLOR_DOUJINSHI
                MANGA -> BG_COLOR_MANGA
                ARTIST_CG -> BG_COLOR_ARTIST_CG
                GAME_CG -> BG_COLOR_GAME_CG
                WESTERN -> BG_COLOR_WESTERN
                NON_H -> BG_COLOR_NON_H
                IMAGE_SET -> BG_COLOR_IMAGE_SET
                COSPLAY -> BG_COLOR_COSPLAY
                ASIAN_PORN -> BG_COLOR_ASIAN_PORN
                MISC -> BG_COLOR_MISC
                else -> BG_COLOR_UNKNOWN
            }.toInt(),
        )
        return if (Settings.harmonizeCategoryColor) {
            val primaryContainer = MaterialTheme.colorScheme.primaryContainer
            mergeColor(primaryContainer, primary)
        } else {
            primary
        }
    }

    val categoryTextColor = Color(0xffe6e0e9)

    val favoriteIconColor = Color(0xffff3040)

    fun signOut() {
        EhCookieStore.removeAllCookies()
        Settings.displayName.value = null
        Settings.hasSignedIn.value = false
        Settings.gallerySite.value = EhUrl.SITE_E
        Settings.needSignIn.value = true
    }

    fun getSuitableTitle(gi: GalleryInfo): String = if (Settings.showJpnTitle) {
        if (gi.titleJpn.isNullOrEmpty()) gi.title else gi.titleJpn
    } else {
        if (gi.title.isNullOrEmpty()) gi.titleJpn else gi.title
    }.orEmpty()

    fun extractTitle(fullTitle: String?): String? {
        var title: String = fullTitle ?: return null
        title = PATTERN_TITLE_PREFIX.replaceFirst(title, "")
        title = PATTERN_TITLE_SUFFIX.replaceFirst(title, "")
        // Sometimes title is combined by romaji and english translation.
        // Only need romaji.
        // TODO But not sure every '|' means that
        return title.substringBeforeLast('|').trim().ifEmpty { null }
    }

    context(Context)
    suspend fun downloadArchive(galleryDetail: GalleryDetail, archive: Archive) {
        val gid = galleryDetail.gid
        EhEngine.downloadArchive(gid, galleryDetail.token, archive.res, archive.isHAtH)?.let {
            val uri = it.toUri()
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, "application/zip")
            }
            val name = "$gid-${getSuitableTitle(galleryDetail)}.zip"
            try {
                startActivity(intent)
                withUIContext { addTextToClipboard(name, true) }
            } catch (_: ActivityNotFoundException) {
                val r = DownloadManager.Request(uri)
                r.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    AppConfig.APP_DIRNAME + "/" + FileUtils.sanitizeFilename(name),
                )
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                downloadManager.enqueue(r)
            }
            if (Settings.archiveMetadata) {
                SpiderDen(galleryDetail).apply {
                    initDownloadDir()
                    writeComicInfo()
                }
            }
        }
    }
}
