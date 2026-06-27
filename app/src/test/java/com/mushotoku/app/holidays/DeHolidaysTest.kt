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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DeHolidaysTest {

    private fun region(iso: String) = DeHolidays.regions.first { it.subdivisionIso == iso }

    private fun datesIn(iso: String, year: Int): Set<LocalDate> =
        region(iso).rules.mapNotNull { it.dateIn(year) }.toSet()

    private fun nameOn(iso: String, date: LocalDate): String? =
        region(iso).rules.firstOrNull { it.dateIn(date.year) == date }?.nameKey

    @Test fun all_sixteen_regions_present() {
        assertEquals(16, DeHolidays.regions.size)
    }

    @Test fun national_days_in_every_region() {
        for (r in DeHolidays.regions) {
            assertTrue(r.subdivisionIso ?: "", r.rules.any { it.dateIn(2025) == LocalDate.of(2025, 10, 3) })
        }
    }

    @Test fun bayern_has_fronleichnam_and_mariae_himmelfahrt_2025() {
        val by = datesIn("DE-BY", 2025)
        assertTrue(by.contains(LocalDate.of(2025, 6, 19)))
        assertTrue(by.contains(LocalDate.of(2025, 8, 15)))
        assertTrue(by.contains(LocalDate.of(2025, 1, 6)))
    }

    @Test fun berlin_has_frauentag_but_not_fronleichnam() {
        val be = datesIn("DE-BE", 2025)
        assertTrue(be.contains(LocalDate.of(2025, 3, 8)))
        assertFalse(be.contains(LocalDate.of(2025, 6, 19)))
    }

    @Test fun sachsen_has_buss_und_bettag() {
        assertEquals("holiday_de_buss_und_bettag", nameOn("DE-SN", LocalDate.of(2025, 11, 19)))
        assertFalse(datesIn("DE-BY", 2025).contains(LocalDate.of(2025, 11, 19)))
    }

    @Test fun thueringen_has_weltkindertag() {
        assertTrue(datesIn("DE-TH", 2025).contains(LocalDate.of(2025, 9, 20)))
    }

    @Test fun reformationstag_in_northern_states_not_in_bw() {
        assertTrue(datesIn("DE-NI", 2025).contains(LocalDate.of(2025, 10, 31)))
        assertFalse(datesIn("DE-BW", 2025).contains(LocalDate.of(2025, 10, 31)))
    }

    @Test fun fronleichnam_moves_with_easter_across_years() {
        assertTrue(datesIn("DE-NW", 2026).contains(LocalDate.of(2026, 6, 4)))
        assertTrue(datesIn("DE-NW", 2027).contains(LocalDate.of(2027, 5, 27)))
    }

    @Test fun catalog_germany_nationwide_default_excludes_regional() {
        val de = HolidayCatalog.byIso("DE")!!
        assertTrue(de.hasSubdivisions)
        val nationwide = de.regionFor(null).rules.mapNotNull { it.dateIn(2025) }.toSet()
        assertTrue(nationwide.contains(LocalDate.of(2025, 10, 3)))
        assertFalse(nationwide.contains(LocalDate.of(2025, 6, 19)))
    }

    @Test fun catalog_has_all_supported_countries() {
        val isos = HolidayCatalog.countries.map { it.iso }
        assertEquals(
            listOf("DE", "AT", "CH", "FR", "IT", "ES", "NL", "PL", "GB", "US", "PT", "BR"),
            isos
        )
    }

    @Test fun portugal_national_days() {
        val pt = HolidayCatalog.byIso("PT")!!.regionFor(null).rules.mapNotNull { it.dateIn(2025) }.toSet()
        assertTrue(pt.contains(LocalDate.of(2025, 6, 10)))
        assertTrue(pt.contains(LocalDate.of(2025, 6, 19)))
        assertTrue(pt.contains(LocalDate.of(2025, 12, 1)))
    }

    @Test fun brazil_consciencia_negra_only_from_2024() {
        val rule = BrHolidays.rules.first { it.nameKey == "holiday_br_consciencia_negra" }
        assertEquals(null, rule.dateIn(2023))
        assertEquals(LocalDate.of(2024, 11, 20), rule.dateIn(2024))
        val br = HolidayCatalog.byIso("BR")!!.regionFor(null).rules.mapNotNull { it.dateIn(2025) }.toSet()
        assertTrue(br.contains(LocalDate.of(2025, 4, 21)))
    }
}
