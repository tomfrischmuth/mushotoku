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

package com.mushotoku.app.finance

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ExpenseProjectionTest {

    private val today = LocalDate.of(2026, 6, 17)

    private fun habit(d: LocalDate, cents: Long) = ExpenseEntry(d, cents, ExpenseKind.HABITUAL)

    @Test fun `monatliches Abo wird mal 12 gerechnet`() {
        val rules = listOf(SubscriptionRule(999, RecurrenceInterval.MONTHLY))
        assertEquals(11988L, ExpenseProjection.annualSubscriptionCost(rules))
    }

    @Test fun `gemischte Abos summieren exakt`() {
        val rules = listOf(
            SubscriptionRule(999, RecurrenceInterval.MONTHLY),
            SubscriptionRule(5999, RecurrenceInterval.YEARLY),
            SubscriptionRule(1299, RecurrenceInterval.MONTHLY),
        )
        assertEquals(33575L, ExpenseProjection.annualSubscriptionCost(rules))
    }

    @Test fun `volle Jahreshistorie liefert reale Summe mit Faktor 1`() {
        val exp = buildList {
            var d = LocalDate.of(2025, 4, 1)
            while (!d.isAfter(today)) {
                add(habit(d, 10000))
                d = d.plusMonths(1)
            }
        }
        val p = ExpenseProjection.projectHabitualMeanRate(exp, today)
        assertEquals(ProjectionBasis.FULL_YEAR, p.basis)
        assertEquals(120000L, p.totalCents)
    }

    @Test fun `wenig Historie ist instabil und wird als EXTRAPOLATED markiert`() {
        val refDay = LocalDate.of(2026, 1, 15)
        val exp = listOf(
            habit(LocalDate.of(2026, 1, 5), 2000),
            habit(LocalDate.of(2026, 1, 10), 1500),
            habit(LocalDate.of(2026, 1, 15), 1500),
        )
        val p = ExpenseProjection.projectHabitualMeanRate(exp, refDay)
        assertEquals(ProjectionBasis.EXTRAPOLATED, p.basis)
        assertEquals(10L, p.sampleSpanDays)
        assertEquals(182500L, p.totalCents)
    }

    @Test fun `90 Tage werden ueber die echte Spanne hochgerechnet`() {
        val exp = buildList {
            var d = today.minusDays(89)
            while (!d.isAfter(today)) { add(habit(d, 500)); d = d.plusDays(1) }
        }
        val p = ExpenseProjection.projectHabitualMeanRate(exp, today)
        assertEquals(ProjectionBasis.EXTRAPOLATED, p.basis)
        assertEquals(89L, p.sampleSpanDays)
        assertEquals(184550L, p.totalCents)
    }

    @Test fun `keine Daten ergibt NO_DATA und null`() {
        val p = ExpenseProjection.projectHabitualMeanRate(emptyList(), today)
        assertEquals(ProjectionBasis.NO_DATA, p.basis)
        assertEquals(0L, p.totalCents)
    }

    @Test fun `ein Ausreisser-Monat zieht den Mittelwert hoch, den Median nicht`() {
        val months = listOf(
            2025 to 6, 2025 to 7, 2025 to 8, 2025 to 9, 2025 to 10, 2025 to 11, 2025 to 12,
            2026 to 1, 2026 to 2, 2026 to 3, 2026 to 4, 2026 to 5,
        )
        val exp = months.map { (y, m) ->
            val cents = if (y == 2025 && m == 11) 90000L else 10000L
            habit(LocalDate.of(y, m, 17), cents)
        }
        val mean   = ExpenseProjection.projectHabitualMeanRate(exp, today)
        val median = ExpenseProjection.projectHabitualMedianMonthly(exp, today)
        assertEquals(ProjectionBasis.FULL_YEAR, mean.basis)
        assertEquals(200000L, mean.totalCents)
        assertEquals(120000L, median.totalCents)
        assertEquals(ProjectionBasis.MEDIAN_PARTIAL, median.basis)
        assertEquals(11, median.sampleMonths)
    }

    @Test fun `bekannte Grenze - Median unterschaetzt unregelmaessige Kategorien`() {
        val exp = listOf(
            habit(LocalDate.of(2025, 12, 5), 6000),
            habit(LocalDate.of(2026, 2, 5), 6000),
            habit(LocalDate.of(2026, 4, 5), 6000),
        )
        val p = ExpenseProjection.projectHabitualMedianMonthly(exp, today)
        assertEquals(0L, p.totalCents)
    }

    @Test fun `einmalige Ausgabe zaehlt zum Nennwert, nicht annualisiert`() {
        val exp = listOf(ExpenseEntry(LocalDate.of(2025, 8, 1), 80000, ExpenseKind.ONE_OFF))
        assertEquals(80000L, ExpenseProjection.oneOffWindowTotal(exp, today))
    }

    @Test fun `Gesamtobergrenze schluesselt sauber auf`() {
        val exp = listOf(
            habit(today.minusDays(30), 3000),
            ExpenseEntry(today.minusDays(60), 50000, ExpenseKind.ONE_OFF),
        )
        val subs = listOf(SubscriptionRule(999, RecurrenceInterval.MONTHLY))
        val c = ExpenseProjection.annualSavingsCeiling(exp, subs, today)
        assertEquals(11988L, c.subscriptionsCents)
        assertEquals(50000L, c.oneOffCents)
        assertEquals(c.subscriptionsCents + c.weeklyUpperCents + c.oneOffCents, c.upperCents)
        assert(c.upperCents >= c.lowerCents)
    }

    @Test fun `projectWeekly vollstaendige Woche gibt exakte Hochrechnung`() {
        val exp = (0L..6L).map { habit(today.minusDays(6 - it), 1000) }
        val p = ExpenseProjection.projectWeekly(exp, today)
        assertEquals(false, p.hasMissingWeekdays)
        assertEquals(p.upperCents, p.lowerCents)
        assertEquals(365000L, p.upperCents)
    }

    @Test fun `projectWeekly fehlende Wochentage erzeugen Spanne`() {
        val exp = listOf(habit(today, 700))
        val p = ExpenseProjection.projectWeekly(exp, today)
        assertEquals(true, p.hasMissingWeekdays)
        assert(p.upperCents > p.lowerCents)
        assertEquals(Math.round(700.0 * 365.0 / 7.0), p.lowerCents)
    }

    @Test fun `projectWeekly NullTage zaehlen als echte Beobachtung`() {
        val exp = listOf(habit(today, 1400))
        val yesterday = today.minusDays(1)
        val exp2 = listOf(habit(today, 1400), habit(yesterday, 0))
        val p1 = ExpenseProjection.projectWeekly(listOf(habit(today, 1400)), today)
        val p2 = ExpenseProjection.projectWeekly(exp2, today)
        assert(p2.lowerCents <= p1.lowerCents)
    }

    @Test fun `TRANSFER_CATEGORY_IDS enthaelt sparbetrag und investitionen, nicht kredit`() {
        assert("sparbetrag" in TRANSFER_CATEGORY_IDS)
        assert("altersvorsorge" in TRANSFER_CATEGORY_IDS)
        assert("investitionen" in TRANSFER_CATEGORY_IDS)
        assert("kredit" !in TRANSFER_CATEGORY_IDS)
    }
}
