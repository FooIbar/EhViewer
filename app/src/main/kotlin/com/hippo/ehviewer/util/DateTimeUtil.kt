package com.hippo.ehviewer.util

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun LocalDate.toEpochMillis(timeZone: TimeZone = TimeZone.UTC): Long =
    atStartOfDayIn(timeZone).toEpochMilliseconds()

fun LocalDateTime.toEpochMillis(timeZone: TimeZone = TimeZone.UTC): Long =
    toInstant(timeZone).toEpochMilliseconds()

fun Long.toLocalDateTime(timeZone: TimeZone = TimeZone.UTC): LocalDateTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone)

// yyyy-MM-dd HH-mm
val DATE_FORMAT_WITHOUT_SECONDS = LocalDateTime.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    dayOfMonth()
    char(' ')
    hour()
    char('-')
    minute()
}
