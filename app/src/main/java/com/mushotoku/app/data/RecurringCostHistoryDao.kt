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
interface RecurringCostHistoryDao {
    @Query("SELECT * FROM recurring_cost_history ORDER BY startMonth DESC")
    fun getAll(): Flow<List<RecurringCostHistory>>

    @Query("SELECT * FROM recurring_cost_history WHERE categoryId = :categoryId AND endMonth IS NULL LIMIT 1")
    suspend fun getActiveForCategory(categoryId: String): RecurringCostHistory?

    @Insert
    suspend fun insert(entry: RecurringCostHistory): Long

    @Update
    suspend fun update(entry: RecurringCostHistory)

    @Query("DELETE FROM recurring_cost_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recurring_cost_history WHERE categoryId = :categoryId")
    suspend fun deleteForCategory(categoryId: String)

    @Query("DELETE FROM recurring_cost_history")
    suspend fun deleteAll()
}
