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
interface MoodDao {
    @Query("SELECT * FROM mood_entries WHERE date = :epochDay")
    fun observeForDate(epochDay: Long): Flow<MoodEntry?>

    @Query("SELECT * FROM mood_entries ORDER BY date DESC LIMIT 30")
    fun observeRecent(): Flow<List<MoodEntry>>

    @Query("SELECT * FROM mood_entries ORDER BY date DESC")
    fun observeAll(): Flow<List<MoodEntry>>

    @Query("SELECT * FROM mood_entries ORDER BY date ASC")
    suspend fun getAllOnce(): List<MoodEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MoodEntry)

    @Query("DELETE FROM mood_entries")
    suspend fun deleteAll()
}
