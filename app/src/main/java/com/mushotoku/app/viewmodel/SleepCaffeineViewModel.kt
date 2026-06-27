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

package com.mushotoku.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mushotoku.app.data.AppDatabase
import com.mushotoku.app.data.CaffeineDose
import com.mushotoku.app.science.CaffeineModel
import com.mushotoku.app.science.Metabolism
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.pow
import kotlin.math.roundToInt

data class CaffeineHint(
    val hasCaffeineToday: Boolean,
    val remainingAtBedMg: Int,
    val overThreshold: Boolean,
    val budgetNowMg: Int,
    val bedtime: LocalTime,
    val cutoffTime: LocalTime?,
)

class SleepCaffeineViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val zone: ZoneId = ZoneId.systemDefault()

    private val refMillis: Long =
        LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    val nowMillis: Long = System.currentTimeMillis()

    private fun millisToHour(m: Long): Double = (m - refMillis) / 3_600_000.0

    val doses: StateFlow<ImmutableList<CaffeineDose>> =
        db.caffeineDoseDao().observeRecent()
            .map { it.toImmutableList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    private val settings = db.appSettingsDao().observe()

    val desiredBedtime: StateFlow<LocalTime> =
        settings.map { LocalTime.ofSecondOfDay(((it?.desiredBedtimeMinutes ?: (23 * 60)) * 60).toLong()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalTime.of(23, 0))

    val metabolism: StateFlow<Metabolism> =
        settings.map { runCatching { Metabolism.valueOf(it?.caffeineMetabolism ?: "NORMAL") }.getOrDefault(Metabolism.NORMAL) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Metabolism.NORMAL)

    val hint: StateFlow<CaffeineHint> =
        combine(doses, desiredBedtime, metabolism) { ds, bedtime, metab ->
            buildHint(ds, bedtime, metab)
        }.flowOn(Dispatchers.Default).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            buildHint(emptyList(), LocalTime.of(23, 0), Metabolism.NORMAL),

        )

    private fun buildHint(ds: List<CaffeineDose>, bedtime: LocalTime, metab: Metabolism): CaffeineHint {
        val halfLife = metab.halfLifeHours
        val bedHour = nextOccurrenceHour(bedtime)
        val nowHour = millisToHour(nowMillis)

        val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val todayDoses = ds.filter { it.timeMillis >= todayStart }
            .map { CaffeineModel.Dose(millisToHour(it.timeMillis), it.amountMg.toDouble()) }

        val remaining = CaffeineModel.remainingMgAt(todayDoses, bedHour, halfLife)
        val overThreshold = remaining >= CaffeineModel.THRESHOLD_MG

        val budget = CaffeineModel.THRESHOLD_MG - remaining
        val decayFactor = 0.5.pow((bedHour - nowHour).coerceAtLeast(0.0) / halfLife)
        val budgetNowMg = if (!overThreshold && decayFactor > 0.0)
            (budget / decayFactor).toInt()
        else 0

        val cutoffHour = if (!overThreshold && budget > 0)
            CaffeineModel.cutoffHour(bedHour, CaffeineModel.MIN_DOSE_MG, budget, halfLife)
        else Double.NEGATIVE_INFINITY
        val cutoffTime = if (cutoffHour > nowHour) hourToLocalTime(cutoffHour) else null

        return CaffeineHint(
            hasCaffeineToday = todayDoses.isNotEmpty(),
            remainingAtBedMg = remaining.roundToInt(),
            overThreshold = overThreshold,
            budgetNowMg = budgetNowMg,
            bedtime = bedtime,
            cutoffTime = cutoffTime,
        )
    }

    private fun nextOccurrenceHour(time: LocalTime): Double {
        val today = LocalDate.now(zone).atTime(time).atZone(zone).toInstant().toEpochMilli()
        val m = if (today > nowMillis) today else today + 24 * 3_600_000L
        return millisToHour(m)
    }

    private fun hourToLocalTime(h: Double): LocalTime {
        val millis = refMillis + (h * 3_600_000.0).toLong()
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
    }

    fun addDose(amountMg: Int, source: String, atMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            db.caffeineDoseDao().upsert(CaffeineDose(timeMillis = atMillis, amountMg = amountMg, source = source))
        }
    }

    fun deleteDose(id: Long) {
        viewModelScope.launch { db.caffeineDoseDao().deleteById(id) }
    }

    fun updateDoseTime(dose: CaffeineDose, time: LocalTime) {
        val date = Instant.ofEpochMilli(dose.timeMillis).atZone(zone).toLocalDate()
        val newMillis = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
        viewModelScope.launch { db.caffeineDoseDao().upsert(dose.copy(timeMillis = newMillis)) }
    }

    fun setDesiredBedtime(time: LocalTime) {
        viewModelScope.launch {
            val current = settings.first() ?: return@launch
            db.appSettingsDao().update(current.copy(desiredBedtimeMinutes = time.hour * 60 + time.minute))
        }
    }

    fun setMetabolism(metab: Metabolism) {
        viewModelScope.launch {
            val current = settings.first() ?: return@launch
            db.appSettingsDao().update(current.copy(caffeineMetabolism = metab.name))
        }
    }
}
