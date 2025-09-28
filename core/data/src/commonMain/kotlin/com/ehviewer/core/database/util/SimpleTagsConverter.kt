package com.ehviewer.core.database.util

import androidx.room.TypeConverter

class SimpleTagsConverter {
    @TypeConverter
    fun fromString(value: String) = value.split(", ")

    @TypeConverter
    fun toString(value: List<String>) = value.joinToString()
}
