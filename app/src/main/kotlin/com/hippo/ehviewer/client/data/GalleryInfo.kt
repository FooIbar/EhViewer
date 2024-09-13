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
package com.hippo.ehviewer.client.data

import moe.tarsin.kt.unreachable

interface GalleryInfo {
    var gid: Long
    var token: String
    var title: String?
    var titleJpn: String?
    var thumbKey: String?
    var category: Int
    var posted: String?
    var uploader: String?
    var disowned: Boolean
    var rating: Float
    var rated: Boolean
    var simpleTags: List<String>?
    var pages: Int
    var thumbWidth: Int
    var thumbHeight: Int
    var simpleLanguage: String?
    var favoriteSlot: Int
    var favoriteName: String?
    var favoriteNote: String?

    fun generateSLang() {
        simpleLanguage = simpleTags?.let { generateSLangFromTags(it) }
            ?: title?.let { generateSLangFromTitle(it) }
    }

    companion object {
        val S_LANGS = arrayOf(
            "EN",
            "ZH",
            "ES",
            "KO",
            "RU",
            "FR",
            "PT",
            "TH",
            "DE",
            "IT",
            "VI",
            "PL",
            "HU",
            "NL",
        )
        private val S_LANG_PATTERNS = arrayOf(
            Regex(
                "[(\\[]eng(?:lish)?[)\\]]|英訳",
                RegexOption.IGNORE_CASE,
            ),
            // [(（\[]ch(?:inese)?[)）\]]|[汉漢]化|中[国國][语語]|中文|中国翻訳
            Regex(
                "[(\uFF08\\[]ch(?:inese)?[)\uFF09\\]]|[汉漢]化|中[国國][语語]|中文|中国翻訳",
                RegexOption.IGNORE_CASE,
            ),
            Regex(
                "[(\\[]spanish[)\\]]|[(\\[]Español[)\\]]|スペイン翻訳",
                RegexOption.IGNORE_CASE,
            ),
            Regex("[(\\[]korean?[)\\]]|韓国翻訳", RegexOption.IGNORE_CASE),
            Regex("[(\\[]rus(?:sian)?[)\\]]|ロシア翻訳", RegexOption.IGNORE_CASE),
            Regex("[(\\[]fr(?:ench)?[)\\]]|フランス翻訳", RegexOption.IGNORE_CASE),
            Regex("[(\\[]portuguese|ポルトガル翻訳", RegexOption.IGNORE_CASE),
            Regex(
                "[(\\[]thai(?: ภาษาไทย)?[)\\]]|แปลไทย|タイ翻訳",
                RegexOption.IGNORE_CASE,
            ),
            Regex("[(\\[]german[)\\]]|ドイツ翻訳", RegexOption.IGNORE_CASE),
            Regex("[(\\[]italiano?[)\\]]|イタリア翻訳", RegexOption.IGNORE_CASE),
            Regex(
                "[(\\[]vietnamese(?: Tiếng Việt)?[)\\]]|ベトナム翻訳",
                RegexOption.IGNORE_CASE,
            ),
            Regex("[(\\[]polish[)\\]]|ポーランド翻訳", RegexOption.IGNORE_CASE),
            Regex("[(\\[]hun(?:garian)?[)\\]]|ハンガリー翻訳", RegexOption.IGNORE_CASE),
            Regex("[(\\[]dutch[)\\]]|オランダ翻訳", RegexOption.IGNORE_CASE),
        )
        val S_LANG_TAGS = arrayOf(
            "language:english",
            "language:chinese",
            "language:spanish",
            "language:korean",
            "language:russian",
            "language:french",
            "language:portuguese",
            "language:thai",
            "language:german",
            "language:italian",
            "language:vietnamese",
            "language:polish",
            "language:hungarian",
            "language:dutch",
        )

        private fun generateSLangFromTags(simpleTags: List<String>): String? {
            for (tag in simpleTags) {
                for (i in S_LANGS.indices) {
                    if (S_LANG_TAGS[i] == tag) {
                        return S_LANGS[i]
                    }
                }
            }
            return null
        }

        private fun generateSLangFromTitle(title: String): String? {
            for (i in S_LANGS.indices) {
                if (S_LANG_PATTERNS[i].containsMatchIn(title)) {
                    return S_LANGS[i]
                }
            }
            return null
        }

        const val NOT_FAVORITED = -2
        const val LOCAL_FAVORITED = -1
    }
}

fun GalleryInfo.findBaseInfo(): BaseGalleryInfo = when (this) {
    is BaseGalleryInfo -> this
    is GalleryDetail -> galleryInfo
    else -> unreachable()
}

fun GalleryInfo.asGalleryDetail(): GalleryDetail? = this as? GalleryDetail

val GalleryInfo.hasAds
    get() = simpleTags?.any { "extraneous ads" in it } ?: false
