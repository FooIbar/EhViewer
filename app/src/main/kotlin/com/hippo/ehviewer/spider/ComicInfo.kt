package com.hippo.ehviewer.spider

import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.TagNamespace
import com.hippo.unifile.UniFile
import com.hippo.unifile.openInputStream
import com.hippo.unifile.openOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.newGenericWriter
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.xmlStreaming

const val COMIC_INFO_FILE = "ComicInfo.xml"
private const val TAG_ORIGINAL = "original"

private val xml = XML {
    recommended {
        ignoreUnknownChildren()
    }
    xmlDeclMode = XmlDeclMode.Charset
    xmlVersion = XmlVersion.XML10
    indent = 2
}

fun GalleryInfo.getComicInfo(): ComicInfo {
    val artists = mutableListOf<String>()
    val groups = mutableListOf<String>()
    val characters = mutableListOf<String>()
    val parodies = mutableListOf<String>()
    val otherTags = mutableListOf<String>()
    with(TagNamespace) {
        when (this@getComicInfo) {
            is GalleryDetail -> tags.forEach { tagList ->
                when (val ns = TagNamespace(tagList.groupName)) {
                    Artist, Cosplayer -> artists.addAll(tagList)
                    Group -> groups.addAll(tagList)
                    Character -> characters.addAll(tagList)
                    Parody -> tagList.forEach { tag -> if (tag != TAG_ORIGINAL) parodies.add(tag) }
                    Other -> otherTags.addAll(tagList)
                    Female, Male, Mixed -> ns.toPrefix()?.let { prefix ->
                        tagList.forEach { tag -> otherTags.add("$prefix:$tag") }
                    }

                    else -> Unit
                }
            }

            else -> simpleTags?.forEach { tagString ->
                val (namespace, tag) = tagString.split(':', limit = 2)
                when (val ns = TagNamespace(namespace)) {
                    Artist, Cosplayer -> artists.add(tag)
                    Group -> groups.add(tag)
                    Character -> characters.add(tag)
                    Parody -> if (tag != TAG_ORIGINAL) parodies.add(tag)
                    Other -> otherTags.add(tag)
                    Female, Male, Mixed -> ns.toPrefix()?.let { otherTags.add("$it:$tag") }
                    else -> Unit
                }
            }
        }
    }
    return ComicInfo(
        series = title,
        alternateSeries = titleJpn.takeUnless { it.isNullOrBlank() },
        writer = groups.takeUnless { it.isEmpty() }?.joinToString(),
        penciller = artists.takeUnless { it.isEmpty() }?.joinToString(),
        genre = otherTags.takeUnless { it.isEmpty() }?.joinToString(),
        web = EhUrl.getGalleryDetailUrl(gid, token),
        pageCount = pages,
        languageISO = simpleLanguage?.lowercase(),
        characters = characters.takeUnless { it.isEmpty() }?.joinToString(),
        teams = parodies.takeUnless { it.isEmpty() }?.joinToString(),
        communityRating = "%.1f".format(rating),
    )
}

fun ComicInfo.write(file: UniFile) {
    file.openOutputStream().use {
        it.channel.truncate(0)
        OutputStreamWriter(it, Charsets.UTF_8).use { writer ->
            xmlStreaming.newGenericWriter(writer).use { xmlWriter ->
                xml.encodeToWriter(xmlWriter, ComicInfo.serializer(), this)
            }
        }
    }
}

fun readComicInfo(file: UniFile): ComicInfo? = runCatching {
    file.openInputStream().use {
        InputStreamReader(it, Charsets.UTF_8).use { reader ->
            xmlStreaming.newGenericReader(reader).use { xmlReader ->
                xml.decodeFromReader(ComicInfo.serializer(), xmlReader)
            }
        }
    }
}.getOrNull()

@Serializable
data class ComicInfo(
    @XmlElement
    @SerialName("Series")
    val series: String?,

    @XmlElement
    @SerialName("AlternateSeries")
    val alternateSeries: String?,

    @XmlElement
    @SerialName("Writer")
    val writer: String?,

    @XmlElement
    @SerialName("Penciller")
    val penciller: String?,

    @XmlElement
    @SerialName("Genre")
    val genre: String?,

    @XmlElement
    @SerialName("Web")
    val web: String?,

    @XmlElement
    @SerialName("PageCount")
    val pageCount: Int = 0,

    @XmlElement
    @SerialName("LanguageISO")
    val languageISO: String?,

    @XmlElement
    @SerialName("Characters")
    val characters: String?,

    @XmlElement
    @SerialName("Teams")
    val teams: String?,

    @XmlElement
    @SerialName("CommunityRating")
    val communityRating: String?,
) {
    @SerialName("xmlns:xsi")
    val xmlSchemaInstance: String = "http://www.w3.org/2001/XMLSchema-instance"

    @SerialName("xsi:noNamespaceSchemaLocation")
    val xmlSchemaLocation: String = "https://raw.githubusercontent.com/anansi-project/comicinfo/main/schema/v2.0/ComicInfo.xsd"
}
