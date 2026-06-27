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

package com.mushotoku.app

import com.mushotoku.app.science.CaffeineModel
import com.mushotoku.app.science.CaffeineModel.Dose
import com.mushotoku.app.science.Metabolism
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ln

class CaffeineModelTest {

    private val halfLife = Metabolism.NORMAL.halfLifeHours

    @Test
    fun remaining_isFullDoseAtIntakeTime() {
        val doses = listOf(Dose(hour = 8.0, amountMg = 100.0))
        assertEquals(100.0, CaffeineModel.remainingMgAt(doses, t = 8.0, halfLife), 1e-9)
    }

    @Test
    fun remaining_halvesEveryHalfLife() {
        val doses = listOf(Dose(hour = 0.0, amountMg = 100.0))
        assertEquals(50.0, CaffeineModel.remainingMgAt(doses, t = halfLife, halfLife), 1e-9)
        assertEquals(25.0, CaffeineModel.remainingMgAt(doses, t = 2 * halfLife, halfLife), 1e-9)
    }

    @Test
    fun remaining_decaysMonotonically() {
        val doses = listOf(Dose(hour = 0.0, amountMg = 95.0))
        var prev = Double.MAX_VALUE
        var t = 0.0
        while (t <= 24.0) {
            val c = CaffeineModel.remainingMgAt(doses, t, halfLife)
            assertTrue("Koffein darf nach der Einnahme nicht steigen", c <= prev + 1e-9)
            prev = c
            t += 0.5
        }
    }

    @Test
    fun remaining_isLinearlySuperposable() {
        val d0 = Dose(0.0, 100.0)
        val d1 = Dose(1.0, 80.0)
        val combined = CaffeineModel.remainingMgAt(listOf(d0, d1), t = 4.0, halfLife)
        val sum = CaffeineModel.remainingMgAt(listOf(d0), 4.0, halfLife) +
            CaffeineModel.remainingMgAt(listOf(d1), 4.0, halfLife)
        assertEquals(sum, combined, 1e-9)
    }

    @Test
    fun remaining_ignoresFutureDoses() {
        val doses = listOf(Dose(hour = 10.0, amountMg = 100.0))
        assertEquals(0.0, CaffeineModel.remainingMgAt(doses, t = 8.0, halfLife), 1e-9)
    }

    @Test
    fun remaining_fasterMetabolismLeavesLess() {
        val doses = listOf(Dose(0.0, 100.0))
        val fast = CaffeineModel.remainingMgAt(doses, t = 6.0, Metabolism.FAST.halfLifeHours)
        val slow = CaffeineModel.remainingMgAt(doses, t = 6.0, Metabolism.SLOW.halfLifeHours)
        assertTrue("Schneller Stoffwechsel baut mehr ab", fast < slow)
    }

    @Test
    fun cutoff_residualHitsThresholdAtBedtime() {
        val bed = 23.0
        val cutoff = CaffeineModel.cutoffHour(
            bedHour = bed,
            doseMg = CaffeineModel.STANDARD_DOSE_MG,
            thresholdMg = CaffeineModel.THRESHOLD_MG,
            halfLife = halfLife,
        )
        val residual = CaffeineModel.remainingMgAt(
            listOf(Dose(cutoff, CaffeineModel.STANDARD_DOSE_MG)), bed, halfLife,
        )
        assertEquals(CaffeineModel.THRESHOLD_MG, residual, 1e-6)
    }

    @Test
    fun cutoff_isEarlierForLargerDose() {
        val bed = 23.0
        val small = CaffeineModel.cutoffHour(bed, CaffeineModel.MIN_DOSE_MG, CaffeineModel.THRESHOLD_MG, halfLife)
        val large = CaffeineModel.cutoffHour(bed, CaffeineModel.STANDARD_DOSE_MG, CaffeineModel.THRESHOLD_MG, halfLife)
        assertTrue("Größere Dosis muss früher getrunken werden", large < small)
    }

    @Test
    fun cutoff_matchesClosedForm() {
        val bed = 23.0
        val cutoff = CaffeineModel.cutoffHour(bed, 200.0, CaffeineModel.THRESHOLD_MG, halfLife)
        val expected = bed - halfLife * (ln(200.0 / CaffeineModel.THRESHOLD_MG) / ln(2.0))
        assertEquals(expected, cutoff, 1e-9)
    }
}
