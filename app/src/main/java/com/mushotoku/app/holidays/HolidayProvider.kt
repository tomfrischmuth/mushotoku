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

import android.util.LruCache
import java.time.LocalDate

interface HolidayProvider {
    fun holidays(region: HolidayRegion, years: IntRange): List<Holiday>

    fun isHoliday(region: HolidayRegion, date: LocalDate): Boolean

    fun holidayOn(region: HolidayRegion, date: LocalDate): Holiday?
}

class DefaultHolidayProvider(cacheYears: Int = 64) : HolidayProvider {

    private val cache = LruCache<String, List<Holiday>>(cacheYears)

    private fun forYear(region: HolidayRegion, year: Int): List<Holiday> {
        val cacheKey = "${region.key}#$year"
        cache.get(cacheKey)?.let { return it }
        val computed = region.rules
            .mapNotNull { rule -> rule.dateIn(year)?.let { Holiday(it, rule.nameKey) } }
            .distinctBy { it.date to it.nameKey }
            .sortedBy { it.date }
        cache.put(cacheKey, computed)
        return computed
    }

    override fun holidays(region: HolidayRegion, years: IntRange): List<Holiday> =
        years.flatMap { forYear(region, it) }.sortedBy { it.date }

    override fun isHoliday(region: HolidayRegion, date: LocalDate): Boolean =
        forYear(region, date.year).any { it.date == date }

    override fun holidayOn(region: HolidayRegion, date: LocalDate): Holiday? =
        forYear(region, date.year).firstOrNull { it.date == date }
}
