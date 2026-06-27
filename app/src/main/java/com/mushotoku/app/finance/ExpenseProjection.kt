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

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class ExpenseKind { HABITUAL, ONE_OFF }

enum class RecurrenceInterval(val daysPerUnit: Double) {
    WEEKLY(7.0),
    MONTHLY(365.0 / 12.0),
    QUARTERLY(365.0 / 4.0),
    YEARLY(365.0),
}

enum class ProjectionBasis {
    NO_DATA,
    EXTRAPOLATED,
    FULL_YEAR,
    MEDIAN_PARTIAL,
    MEDIAN_FULL,
}

enum class ProjectionMethod { MEAN_RATE, MEDIAN_MONTHLY }

data class ExpenseEntry(
    val date: LocalDate,
    val amountCents: Long,
    val kind: ExpenseKind = ExpenseKind.HABITUAL,
)

data class SubscriptionRule(
    val amountCents: Long,
    val interval: RecurrenceInterval,
    val intervalCount: Int = 1,
) {
    fun annualOccurrences(): Double = 365.0 / (interval.daysPerUnit * intervalCount)
    fun annualCostCents(): Long = Math.round(amountCents * annualOccurrences())
}

data class AnnualProjection(
    val totalCents: Long,
    val basis: ProjectionBasis,
    val sampleSpanDays: Long = 0,
    val sampleMonths: Int = 0,
    val method: ProjectionMethod = ProjectionMethod.MEAN_RATE,
)

data class WeeklyProjection(
    val upperCents: Long,
    val lowerCents: Long,
    val hasMissingWeekdays: Boolean,
    val observedDays: Long,
)

data class SavingsCeiling(
    val upperCents: Long,
    val lowerCents: Long,
    val subscriptionsCents: Long,
    val weeklyUpperCents: Long,
    val weeklyLowerCents: Long,
    val oneOffCents: Long,
    val hasMissingWeekdays: Boolean,
)

val TRANSFER_CATEGORY_IDS: Set<String> = setOf("sparbetrag", "altersvorsorge", "investitionen")

val SAVINGS_ELIGIBLE_CATEGORY_IDS: Set<String> = setOf(
    "restaurant", "cafe", "takeaway", "lieferdienst", "kantine", "bar",
    "events", "ausstellungen", "sportveranstalt", "lesestoff", "kulturveranstalt",
    "fitnessstudio", "sportkurse", "sportverein", "sportausruestung",
    "schwimmbad", "outdoor_sport", "indoor_sport",
    "kleidung", "schuhe", "taschen", "accessoires", "sportkleidung",
    "abonnements", "streaming", "gaming", "software",
)

val REISEN_CATEGORY_IDS: Set<String> = setOf(
    "fluege", "unterkunft", "mietwagen", "aktivitaeten", "souvenirs", "reisegepaeck",
)

object ExpenseProjection {

    private const val WINDOW_DAYS = 365L

    fun annualSubscriptionCost(rules: List<SubscriptionRule>): Long =
        rules.sumOf { it.annualCostCents() }

    fun oneOffWindowTotal(expenses: List<ExpenseEntry>, today: LocalDate): Long {
        val windowStart = today.minusDays(WINDOW_DAYS)
        return expenses
            .filter { it.kind == ExpenseKind.ONE_OFF && it.date in windowStart..today }
            .sumOf { it.amountCents }
    }

    fun projectHabitualMeanRate(expenses: List<ExpenseEntry>, today: LocalDate): AnnualProjection {
        val habitual = expenses.filter { it.kind == ExpenseKind.HABITUAL }
        val windowStart = today.minusDays(WINDOW_DAYS)
        val inWindow = habitual.filter { it.date in windowStart..today }
        if (inWindow.isEmpty()) {
            return AnnualProjection(0, ProjectionBasis.NO_DATA, method = ProjectionMethod.MEAN_RATE)
        }

        val total = inWindow.sumOf { it.amountCents }
        val overallFirst = habitual.minOf { it.date }
        val hasFullYear = ChronoUnit.DAYS.between(overallFirst, today) >= WINDOW_DAYS

        if (hasFullYear) {
            return AnnualProjection(total, ProjectionBasis.FULL_YEAR, sampleSpanDays = WINDOW_DAYS)
        }

        val firstSeen = inWindow.minOf { it.date }
        val span = ChronoUnit.DAYS.between(firstSeen, today).coerceAtLeast(1)
        val projected = total * WINDOW_DAYS / span
        return AnnualProjection(projected, ProjectionBasis.EXTRAPOLATED, sampleSpanDays = span)
    }

    fun projectHabitualMedianMonthly(
        expenses: List<ExpenseEntry>,
        today: LocalDate,
        minMonths: Int = 3,
        maxMonths: Int = 12,
    ): AnnualProjection {
        val habitual = expenses.filter { it.kind == ExpenseKind.HABITUAL }
        if (habitual.isEmpty()) {
            return AnnualProjection(0, ProjectionBasis.NO_DATA, method = ProjectionMethod.MEDIAN_MONTHLY)
        }

        val firstDate = habitual.minOf { it.date }
        val firstComplete =
            if (firstDate.dayOfMonth == 1) YearMonth.from(firstDate)
            else YearMonth.from(firstDate).plusMonths(1)
        val lastComplete = YearMonth.from(today).minusMonths(1)

        if (lastComplete.isBefore(firstComplete)) {
            return projectHabitualMeanRate(expenses, today)
        }

        val rangeStart = maxOf(firstComplete, lastComplete.minusMonths((maxMonths - 1).toLong()))

        val sums = HashMap<YearMonth, Long>()
        var ym = rangeStart
        while (!ym.isAfter(lastComplete)) {
            sums[ym] = 0L
            ym = ym.plusMonths(1)
        }
        for (e in habitual) {
            val m = YearMonth.from(e.date)
            if (!m.isBefore(rangeStart) && !m.isAfter(lastComplete)) {
                sums[m] = (sums[m] ?: 0L) + e.amountCents
            }
        }

        val monthly = sums.values.sorted()
        if (monthly.size < minMonths) {
            return projectHabitualMeanRate(expenses, today)
        }

        val median = medianOf(monthly)
        val basis = if (monthly.size >= 12) ProjectionBasis.MEDIAN_FULL else ProjectionBasis.MEDIAN_PARTIAL
        return AnnualProjection(
            totalCents   = median * 12,
            basis        = basis,
            sampleMonths = monthly.size,
            method       = ProjectionMethod.MEDIAN_MONTHLY,
        )
    }

    fun projectWeekly(expenses: List<ExpenseEntry>, today: LocalDate): WeeklyProjection {
        val habitual = expenses.filter { it.kind == ExpenseKind.HABITUAL }
        if (habitual.isEmpty()) return WeeklyProjection(0, 0, false, 0)

        val firstDate = habitual.minOf { it.date }

        val nD   = LongArray(7)
        val sumD = LongArray(7)

        var d = firstDate
        while (!d.isAfter(today)) {
            nD[d.dayOfWeek.value - 1]++
            d = d.plusDays(1)
        }

        for (exp in habitual) {
            sumD[exp.date.dayOfWeek.value - 1] += exp.amountCents
        }

        val totalN   = nD.sum()
        val totalSum = sumD.sum()
        val globalAvg = if (totalN > 0) totalSum.toDouble() / totalN else 0.0

        val hasMissing = nD.any { it == 0L }
        val factor     = 365.0 / 7.0

        var lowerWeek = 0.0
        var upperWeek = 0.0
        for (i in 0..6) {
            val observed = if (nD[i] > 0) sumD[i].toDouble() / nD[i] else 0.0
            lowerWeek += observed
            upperWeek += if (nD[i] > 0) observed else globalAvg
        }

        return WeeklyProjection(
            upperCents         = Math.round(upperWeek * factor),
            lowerCents         = Math.round(lowerWeek * factor),
            hasMissingWeekdays = hasMissing,
            observedDays       = totalN,
        )
    }

    fun annualSavingsCeiling(
        expenses: List<ExpenseEntry>,
        subscriptions: List<SubscriptionRule>,
        today: LocalDate,
    ): SavingsCeiling {
        val subs   = annualSubscriptionCost(subscriptions)
        val weekly = projectWeekly(expenses, today)
        val oneOff = oneOffWindowTotal(expenses, today)
        return SavingsCeiling(
            upperCents         = subs + weekly.upperCents + oneOff,
            lowerCents         = subs + weekly.lowerCents + oneOff,
            subscriptionsCents = subs,
            weeklyUpperCents   = weekly.upperCents,
            weeklyLowerCents   = weekly.lowerCents,
            oneOffCents        = oneOff,
            hasMissingWeekdays = weekly.hasMissingWeekdays,
        )
    }

    private fun medianOf(sortedAsc: List<Long>): Long {
        val n = sortedAsc.size
        return if (n % 2 == 1) sortedAsc[n / 2]
        else (sortedAsc[n / 2 - 1] + sortedAsc[n / 2]) / 2
    }
}
