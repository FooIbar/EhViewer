package com.hippo.ehviewer.client.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class TagNamespace(val value: String, val prefix: String?) : Parcelable {
    object Artist : TagNamespace("artist", "a")
    object Cosplayer : TagNamespace("cosplayer", "cos")
    object Character : TagNamespace("character", "c")
    object Female : TagNamespace("female", "f")
    object Group : TagNamespace("group", "g")
    object Language : TagNamespace("language", "l")
    object Male : TagNamespace("male", "m")
    object Mixed : TagNamespace("mixed", "x")
    object Other : TagNamespace("other", "o")
    object Parody : TagNamespace("parody", "p")
    object Reclass : TagNamespace("reclass", "r")
    object Temp : TagNamespace("temp", null)

    companion object {
        fun from(value: String) = when (value) {
            Artist.value -> Artist
            Cosplayer.value -> Cosplayer
            Character.value -> Character
            Female.value -> Female
            Group.value -> Group
            Language.value -> Language
            Male.value -> Male
            Mixed.value -> Mixed
            Other.value -> Other
            Parody.value -> Parody
            Reclass.value -> Reclass
            Temp.value -> Temp
            else -> null
        }
    }
}
