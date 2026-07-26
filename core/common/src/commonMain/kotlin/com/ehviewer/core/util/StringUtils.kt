package com.ehviewer.core.util

private val EntityRegex = "&(?:amp|lt|gt|quot|#039|nbsp|times);".toRegex()
private val EntityMap = mapOf(
    "&amp;" to "&",
    "&lt;" to "<",
    "&gt;" to ">",
    "&quot;" to "\"",
    "&#039;" to "'", // HTML uses &#039; instead of &apos;
    "&nbsp;" to " ",
    "&times;" to "×",
)

fun String.unescapeXml() = replace(EntityRegex) { EntityMap[it.value]!! }

fun String.toIntOrDefault(defaultValue: Int): Int = toIntOrNull() ?: defaultValue

fun String?.containsIgnoreCase(other: String) = this?.contains(other, true) == true
