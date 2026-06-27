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
interface CaffeineDoseDao {
    @Query("SELECT * FROM caffeine_doses WHERE timeMillis >= :fromMillis AND timeMillis <= :toMillis ORDER BY timeMillis ASC")
    fun observeInRange(fromMillis: Long, toMillis: Long): Flow<List<CaffeineDose>>

    @Query("SELECT * FROM caffeine_doses ORDER BY timeMillis DESC LIMIT 50")
    fun observeRecent(): Flow<List<CaffeineDose>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dose: CaffeineDose): Long

    @Query("DELETE FROM caffeine_doses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM caffeine_doses")
    suspend fun deleteAll()
}
