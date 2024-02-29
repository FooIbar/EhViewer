/*
 * Copyright 2015 Hippo Seven
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
package com.hippo.ehviewer.util

import com.hippo.ehviewer.R
import java.util.Locale
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import splitties.init.appCtx

object ReadableTime {
    private const val SECOND_MILLIS = 1000L
    private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
    private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    private const val DAY_MILLIS = 24 * HOUR_MILLIS
    private const val WEEK_MILLIS = 7 * DAY_MILLIS
    private const val YEAR_MILLIS = 365 * DAY_MILLIS
    private val MULTIPLES = longArrayOf(
        YEAR_MILLIS,
        DAY_MILLIS,
        HOUR_MILLIS,
        MINUTE_MILLIS,
        SECOND_MILLIS,
    )
    private const val SIZE = 5
    private val UNITS = intArrayOf(
        R.plurals.year,
        R.plurals.day,
        R.plurals.hour,
        R.plurals.minute,
        R.plurals.second,
    )
    private val sCalendarLock = Any()
    private val DATE_FORMAT_WITHOUT_YEAR = LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        dayOfMonth(Padding.NONE)
    }
    private val DATE_FORMAT_WITH_YEAR = LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        dayOfMonth(Padding.NONE)
        chars(", ")
        year()
    }
    private val DATE_FORMAT_WITHOUT_YEAR_ZH = LocalDate.Format {
        monthNumber(Padding.NONE)
        char('月')
        dayOfMonth(Padding.NONE)
        char('日')
    }
    private val DATE_FORMAT_WITH_YEAR_ZH = LocalDate.Format {
        year()
        char('年')
        monthNumber(Padding.NONE)
        char('月')
        dayOfMonth(Padding.NONE)
        char('日')
    }

    // yyyy-MM-dd-HH-mm-ss-SSS
    private val FILENAMABLE_DATE_FORMAT = LocalDateTime.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        dayOfMonth()
        char('-')
        hour()
        char('-')
        minute()
        char('-')
        second()
        char('-')
        secondFraction(3)
    }
    private val sDateFormatLock = Any()
    private val resources = appCtx.resources

    fun getTimeAgo(time: Long): String {
        val nowInstant = Clock.System.now()
        val now = nowInstant.toEpochMilliseconds()
        val diff = now - time
        return when {
            (diff < 0 || time <= 0) -> {
                resources.getString(R.string.from_the_future)
            }
            diff < MINUTE_MILLIS -> {
                resources.getString(R.string.just_now)
            }
            diff < 2 * MINUTE_MILLIS -> {
                resources.getQuantityString(R.plurals.some_minutes_ago, 1, 1)
            }
            diff < 50 * MINUTE_MILLIS -> {
                val minutes = (diff / MINUTE_MILLIS).toInt()
                resources.getQuantityString(R.plurals.some_minutes_ago, minutes, minutes)
            }
            diff < 90 * MINUTE_MILLIS -> {
                resources.getQuantityString(R.plurals.some_hours_ago, 1, 1)
            }
            diff < 24 * HOUR_MILLIS -> {
                val hours = (diff / HOUR_MILLIS).toInt()
                resources.getQuantityString(R.plurals.some_hours_ago, hours, hours)
            }
            diff < 48 * HOUR_MILLIS -> {
                resources.getString(R.string.yesterday)
            }
            diff < WEEK_MILLIS -> {
                val days = (diff / DAY_MILLIS).toInt()
                resources.getString(R.string.some_days_ago, days)
            }
            else -> synchronized(sCalendarLock) {
                val timeZone = TimeZone.currentSystemDefault()
                val nowDate = nowInstant.toLocalDateTime(timeZone).date
                val timeDate = time.toLocalDateTime(timeZone).date
                val nowYear = nowDate.year
                val timeYear = timeDate.year
                val isZh = Locale.getDefault().language == "zh"
                if (nowYear == timeYear) {
                    if (isZh) DATE_FORMAT_WITHOUT_YEAR_ZH else DATE_FORMAT_WITHOUT_YEAR
                } else {
                    if (isZh) DATE_FORMAT_WITH_YEAR_ZH else DATE_FORMAT_WITH_YEAR
                }.format(timeDate)
            }
        }
    }

    fun getShortTimeInterval(time: Long): String {
        return buildString {
            for (i in 0 until SIZE) {
                val multiple = MULTIPLES[i]
                val quotient = time / multiple
                if (time > multiple * 1.5 || i == SIZE - 1) {
                    append(quotient)
                        .append(" ")
                        .append(resources.getQuantityString(UNITS[i], quotient.toInt()))
                    break
                }
            }
        }
    }

    fun getFilenamableTime(time: Long): String {
        synchronized(sDateFormatLock) {
            return FILENAMABLE_DATE_FORMAT.format(
                time.toLocalDateTime(TimeZone.currentSystemDefault()),
            )
        }
    }
}
