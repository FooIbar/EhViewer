package com.hippo.ehviewer.client.data

@JvmInline
value class TagNamespace(val value: String) {
    fun toPrefix(): String? = prefixMap[this]

    companion object {
        val Artist = TagNamespace("artist")
        val Cosplayer = TagNamespace("cosplayer")
        val Character = TagNamespace("character")
        val Female = TagNamespace("female")
        val Group = TagNamespace("group")
        val Language = TagNamespace("language")
        val Male = TagNamespace("male")
        val Mixed = TagNamespace("mixed")
        val Other = TagNamespace("other")
        val Parody = TagNamespace("parody")
        val Reclass = TagNamespace("reclass")
        private val prefixMap = mapOf(
            Artist to "a",
            Cosplayer to "cos",
            Character to "c",
            Female to "f",
            Group to "g",
            Language to "l",
            Male to "m",
            Mixed to "x",
            Other to "o",
            Parody to "p",
            Reclass to "r",
        )
    }
}
