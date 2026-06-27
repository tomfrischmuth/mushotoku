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
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date = :epochDay ORDER BY sortOrder ASC, id ASC")
    fun getTasksForDate(epochDay: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date = :epochDay ORDER BY sortOrder ASC, id ASC")
    suspend fun getTasksForDateOnce(epochDay: Long): List<Task>

    @Query("SELECT * FROM tasks ORDER BY date ASC, sortOrder ASC")
    suspend fun getAllTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getTasksForRange(startDate: Long, endDate: Long): Flow<List<Task>>

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE isAppointment = 0")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM tasks WHERE isAppointment = 1")
    suspend fun deleteAllAppointments()

    @Query("SELECT * FROM tasks WHERE isAppointment = 1 ORDER BY date ASC, time ASC")
    fun getAllAppointments(): Flow<List<Task>>

    @Query("UPDATE tasks SET linkedNoteId = NULL WHERE linkedNoteId = :noteId")
    suspend fun clearLinkedNoteId(noteId: Long)

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): Task?

    @Query("UPDATE tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TaskStatus)
}
