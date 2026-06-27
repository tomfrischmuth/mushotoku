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

package com.mushotoku.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AdditionalIncomeDao {
    @Query("SELECT * FROM additional_incomes WHERE date = :epochDay ORDER BY id ASC")
    fun getForDate(epochDay: Long): Flow<List<AdditionalIncome>>

    @Query("SELECT * FROM additional_incomes WHERE date >= :from AND date <= :to ORDER BY date ASC")
    fun getForRange(from: Long, to: Long): Flow<List<AdditionalIncome>>

    @Insert
    suspend fun insert(entry: AdditionalIncome): Long

    @Delete
    suspend fun delete(entry: AdditionalIncome)

    @Query("DELETE FROM additional_incomes")
    suspend fun deleteAll()
}
