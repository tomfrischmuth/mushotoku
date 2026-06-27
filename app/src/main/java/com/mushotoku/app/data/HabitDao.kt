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
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<Habit>>

    @Query("SELECT habitId FROM habit_logs WHERE date = :epochDay")
    fun observeCompletionsForDate(epochDay: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: Habit)

    @Delete
    suspend fun delete(habit: Habit)

    @Update
    suspend fun update(habit: Habit)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun logCompletion(log: HabitLog)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :epochDay")
    suspend fun removeCompletion(habitId: String, epochDay: Long)

    @Query("SELECT * FROM habit_logs ORDER BY date ASC")
    fun observeAllLogs(): Flow<List<HabitLog>>

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Query("DELETE FROM habit_logs")
    suspend fun deleteAllHabitLogs()
}
