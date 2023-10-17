package com.hippo.ehviewer.util

import org.jsoup.parser.Parser

fun String.unescapeXml(): String {
    return Parser.unescapeEntities(this, true)
}

inline infix fun <T> CharSequence.trimAnd(block: CharSequence.() -> T): T {
    return block(trim())
}

fun String.toIntOrDefault(defaultValue: Int): Int {
    return toIntOrNull() ?: defaultValue
}

fun String.toLongOrDefault(defaultValue: Long): Long {
    return toLongOrNull() ?: defaultValue
}

fun String.toFloatOrDefault(defaultValue: Float): Float {
    return toFloatOrNull() ?: defaultValue
}

fun String?.containsIgnoreCase(other: String) = this?.contains(other, true) ?: false
