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

package com.mushotoku.app.science

import kotlin.math.ln
import kotlin.math.pow

enum class Metabolism(val halfLifeHours: Double) {
    FAST(3.5),
    NORMAL(5.0),
    SLOW(7.0),
}

object CaffeineModel {
    const val STANDARD_DOSE_MG = 95.0

    const val MIN_DOSE_MG = 32.0

    const val THRESHOLD_MG = 25.0

    const val DRAKE_FLOOR_H = 6.0

    data class Dose(val hour: Double, val amountMg: Double)

    fun remainingMgAt(doses: List<Dose>, t: Double, halfLife: Double): Double =
        doses.filter { it.hour <= t }
            .sumOf { it.amountMg * 0.5.pow((t - it.hour) / halfLife) }

    fun cutoffHour(bedHour: Double, doseMg: Double, thresholdMg: Double, halfLife: Double): Double =
        bedHour - halfLife * (ln(doseMg / thresholdMg) / ln(2.0))
}
