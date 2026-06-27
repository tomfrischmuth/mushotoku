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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

class HolidayComputusTest {

    @Test fun easter_known_years() {
        assertEquals(LocalDate.of(2024, 3, 31), easterSunday(2024))
        assertEquals(LocalDate.of(2025, 4, 20), easterSunday(2025))
        assertEquals(LocalDate.of(2026, 4, 5), easterSunday(2026))
        assertEquals(LocalDate.of(2027, 3, 28), easterSunday(2027))
    }

    @Test fun fixed_rule() {
        val neujahr = Fixed(Month.JANUARY, 1, "x")
        assertEquals(LocalDate.of(2025, 1, 1), neujahr.dateIn(2025))
    }

    @Test fun easter_relative_rule() {
        assertEquals(LocalDate.of(2025, 4, 18), EasterRelative(-2, "x").dateIn(2025))
        assertEquals(LocalDate.of(2025, 4, 21), EasterRelative(1, "x").dateIn(2025))
        assertEquals(LocalDate.of(2025, 6, 9), EasterRelative(50, "x").dateIn(2025))
    }

    @Test fun nth_weekday_rule() {
        val thanksgiving = NthWeekday(Month.NOVEMBER, DayOfWeek.THURSDAY, 4, "x")
        assertEquals(LocalDate.of(2025, 11, 27), thanksgiving.dateIn(2025))
        val mlk = NthWeekday(Month.JANUARY, DayOfWeek.MONDAY, 3, "x")
        assertEquals(LocalDate.of(2025, 1, 20), mlk.dateIn(2025))
    }

    @Test fun last_weekday_rule() {
        val springBank = LastWeekday(Month.MAY, DayOfWeek.MONDAY, "x")
        assertEquals(LocalDate.of(2025, 5, 26), springBank.dateIn(2025))
    }

    @Test fun custom_rule_buss_und_bettag() {
        val bbt = Custom("x") { y ->
            LocalDate.of(y, 11, 23).with(java.time.temporal.TemporalAdjusters.previous(DayOfWeek.WEDNESDAY))
        }
        assertEquals(LocalDate.of(2025, 11, 19), bbt.dateIn(2025))
        assertEquals(LocalDate.of(2024, 11, 20), bbt.dateIn(2024))
    }

    @Test fun observed_us_style_shifts_weekend() {
        assertEquals(LocalDate.of(2021, 12, 31), LocalDate.of(2022, 1, 1).observedUsStyle())
        assertEquals(LocalDate.of(2021, 7, 5), LocalDate.of(2021, 7, 4).observedUsStyle())
        assertEquals(LocalDate.of(2025, 7, 4), LocalDate.of(2025, 7, 4).observedUsStyle())
    }

    @Test fun uk_substitute_shifts_weekend_forward() {
        assertEquals(LocalDate.of(2021, 12, 27), LocalDate.of(2021, 12, 25).ukSubstitute())
        assertEquals(LocalDate.of(2021, 12, 27), LocalDate.of(2021, 12, 26).ukSubstitute())
    }

    @Test fun custom_rule_can_return_null() {
        val never = Custom("x") { null }
        assertNull(never.dateIn(2025))
    }
}
