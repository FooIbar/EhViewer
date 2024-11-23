package com.hippo.ehviewer.spider

import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryTag
import com.hippo.ehviewer.client.data.PowerStatus
import com.hippo.ehviewer.client.data.SimpleTagsConverter
import com.hippo.ehviewer.client.data.TagNamespace
import com.hippo.ehviewer.client.data.TagNamespace.Artist
import com.hippo.ehviewer.client.data.TagNamespace.Character
import com.hippo.ehviewer.client.data.TagNamespace.Cosplayer
import com.hippo.ehviewer.client.data.TagNamespace.Female
import com.hippo.ehviewer.client.data.TagNamespace.Group
import com.hippo.ehviewer.client.data.TagNamespace.Male
import com.hippo.ehviewer.client.data.TagNamespace.Mixed
import com.hippo.ehviewer.client.data.TagNamespace.Other
import com.hippo.ehviewer.client.data.TagNamespace.Parody
import com.hippo.files.openInputStream
import com.hippo.files.openOutputStream
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.newWriter
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.xmlStreaming
import okio.Path

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
            is GalleryDetail -> tagGroups.forEach { group ->
                val list = group.tags.filterNot { (text, power, _) -> text == TAG_ORIGINAL || power == PowerStatus.WEAK }.map(GalleryTag::text)
                when (val ns = group.nameSpace) {
                    Artist, Cosplayer -> artists.addAll(list)
                    Group -> groups.addAll(list)
                    Character -> characters.addAll(list)
                    Parody -> parodies.addAll(list)
                    Other -> otherTags.addAll(list)
                    Female, Male, Mixed -> ns.prefix.let { prefix ->
                        list.forEach { tag -> otherTags.add("$prefix:$tag") }
                    }
                    else -> Unit
                }
            }

            else -> simpleTags?.forEach { tagString ->
                val (namespace, tag) = tagString.split(':', limit = 2)
                    .takeIf { it.size == 2 } ?: return@forEach // Ignore temp tags that don't have namespace
                when (val ns = from(namespace)) {
                    Artist, Cosplayer -> artists.add(tag)
                    Group -> groups.add(tag)
                    Character -> characters.add(tag)
                    Parody -> if (tag != TAG_ORIGINAL) parodies.add(tag)
                    Other -> otherTags.add(tag)
                    Female, Male, Mixed -> ns.prefix.let { otherTags.add("$it:$tag") }
                    else -> Unit
                }
            }
        }
    }
    return ComicInfo(
        series = title,
        alternateSeries = titleJpn?.ifBlank { null },
        writer = groups.ifEmpty { null },
        penciller = artists.ifEmpty { null },
        genre = otherTags.ifEmpty { null },
        web = EhUrl.getGalleryDetailUrl(gid, token),
        pageCount = pages,
        languageISO = simpleLanguage?.lowercase(),
        characters = characters.ifEmpty { null },
        teams = parodies.ifEmpty { null },
        communityRating = "%.1f".format(rating),
    )
}

fun ComicInfo.toSimpleTags() = listOfNotNull(
    writer,
    penciller,
    genre,
    characters,
    teams,
).flatten().ifEmpty { null }

fun ComicInfo.write(file: Path) {
    file.openOutputStream().bufferedWriter().use {
        xmlStreaming.newWriter(it).use { writer ->
            xml.encodeToWriter(writer, ComicInfo.serializer(), this)
        }
    }
}

fun readComicInfo(file: Path): ComicInfo? = runCatching {
    file.openInputStream().bufferedReader().use {
        xmlStreaming.newReader(it).use { reader ->
            xml.decodeFromReader(ComicInfo.serializer(), reader)
        }
    }
}.getOrNull()

@Suppress("ktlint:standard:annotation")
typealias SimpleTags = @Serializable(SimpleTagsSerializer::class) List<String>

object SimpleTagsSerializer : KSerializer<SimpleTags> {
    private val converter = SimpleTagsConverter()
    override val descriptor = PrimitiveSerialDescriptor("SimpleTags", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder) = converter.fromString(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: SimpleTags) = encoder.encodeString(converter.toString(value))
}

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
    val writer: SimpleTags?,

    @XmlElement
    @SerialName("Penciller")
    val penciller: SimpleTags?,

    @XmlElement
    @SerialName("Genre")
    val genre: SimpleTags?,

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
    val characters: SimpleTags?,

    @XmlElement
    @SerialName("Teams")
    val teams: SimpleTags?,

    @XmlElement
    @SerialName("CommunityRating")
    val communityRating: String?,
) {
    @SerialName("xmlns:xsi")
    val xmlSchemaInstance: String = "http://www.w3.org/2001/XMLSchema-instance"

    @SerialName("xsi:noNamespaceSchemaLocation")
    val xmlSchemaLocation: String = "https://raw.githubusercontent.com/anansi-project/comicinfo/main/schema/v2.0/ComicInfo.xsd"
}
