/*
 * Mushotoku — a privacy-focused, offline productivity app.
 * Copyright (C) 2026 Tom Frischmuth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mushotoku.app.holidays

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

fun easterSunday(year: Int): LocalDate {
    val a = year % 19
    val b = year / 100
    val c = year % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31
    val day = ((h + l - 7 * m + 114) % 31) + 1
    return LocalDate.of(year, month, day)
}

sealed interface HolidayRule {
    val nameKey: String
    fun dateIn(year: Int): LocalDate?
}

data class Fixed(val month: Month, val day: Int, override val nameKey: String) : HolidayRule {
    override fun dateIn(year: Int): LocalDate = LocalDate.of(year, month, day)
}

data class EasterRelative(val offset: Long, override val nameKey: String) : HolidayRule {
    override fun dateIn(year: Int): LocalDate = easterSunday(year).plusDays(offset)
}

data class NthWeekday(
    val month: Month, val weekday: DayOfWeek, val ordinal: Int, override val nameKey: String
) : HolidayRule {
    override fun dateIn(year: Int): LocalDate =
        LocalDate.of(year, month, 1)
            .with(TemporalAdjusters.firstInMonth(weekday))
            .plusWeeks((ordinal - 1).toLong())
}

data class LastWeekday(
    val month: Month, val weekday: DayOfWeek, override val nameKey: String
) : HolidayRule {
    override fun dateIn(year: Int): LocalDate =
        LocalDate.of(year, month, 1).with(TemporalAdjusters.lastInMonth(weekday))
}

data class Custom(
    override val nameKey: String,
    val resolver: (Int) -> LocalDate?
) : HolidayRule {
    override fun dateIn(year: Int): LocalDate? = resolver(year)
}

fun LocalDate.observedUsStyle(): LocalDate = when (dayOfWeek) {
    DayOfWeek.SATURDAY -> minusDays(1)
    DayOfWeek.SUNDAY -> plusDays(1)
    else -> this
}

fun LocalDate.ukSubstitute(): LocalDate = when (dayOfWeek) {
    DayOfWeek.SATURDAY -> plusDays(2)
    DayOfWeek.SUNDAY -> plusDays(1)
    else -> this
}

data class HolidayRegion(
    val countryIso: String,
    val subdivisionIso: String?,
    val labelKey: String,
    val rules: List<HolidayRule>
) {
    val key: String get() = subdivisionIso ?: countryIso
}

data class Holiday(val date: LocalDate, val nameKey: String)
