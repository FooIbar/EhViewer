package com.hippo.ehviewer.util

import org.jsoup.parser.Parser

fun String.unescapeXml(): String = Parser.unescapeEntities(this, true)

inline infix fun <T> CharSequence.trimAnd(block: CharSequence.() -> T): T = block(trim())

fun String.toIntOrDefault(defaultValue: Int): Int = toIntOrNull() ?: defaultValue

fun String.toLongOrDefault(defaultValue: Long): Long = toLongOrNull() ?: defaultValue

fun String.toFloatOrDefault(defaultValue: Float): Float = toFloatOrNull() ?: defaultValue

fun String?.containsIgnoreCase(other: String) = this?.contains(other, true) ?: false
