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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BackupDao {

    @Query("SELECT * FROM tasks") suspend fun allTasks(): List<Task>
    @Query("SELECT * FROM notes") suspend fun allNotes(): List<Note>
    @Query("SELECT * FROM expenses") suspend fun allExpenses(): List<Expense>
    @Query("SELECT * FROM categories") suspend fun allCategories(): List<Category>
    @Query("SELECT * FROM app_settings WHERE id = 1") suspend fun settings(): AppSettings?
    @Query("SELECT * FROM habits") suspend fun allHabits(): List<Habit>
    @Query("SELECT * FROM habit_logs") suspend fun allHabitLogs(): List<HabitLog>
    @Query("SELECT * FROM gratitude_entries") suspend fun allGratitude(): List<GratitudeEntry>
    @Query("SELECT * FROM mood_entries") suspend fun allMoods(): List<MoodEntry>
    @Query("SELECT * FROM caffeine_doses") suspend fun allCaffeine(): List<CaffeineDose>
    @Query("SELECT * FROM recurring_cost_history") suspend fun allRecurringCostHistory(): List<RecurringCostHistory>
    @Query("SELECT * FROM additional_incomes") suspend fun allAdditionalIncomes(): List<AdditionalIncome>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTasks(rows: List<Task>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertNotes(rows: List<Note>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExpenses(rows: List<Expense>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCategories(rows: List<Category>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSettings(row: AppSettings)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHabits(rows: List<Habit>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHabitLogs(rows: List<HabitLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGratitude(rows: List<GratitudeEntry>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMoods(rows: List<MoodEntry>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCaffeine(rows: List<CaffeineDose>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRecurringCostHistory(rows: List<RecurringCostHistory>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAdditionalIncomes(rows: List<AdditionalIncome>)

    @Query("DELETE FROM tasks") suspend fun clearTasks()
    @Query("DELETE FROM notes") suspend fun clearNotes()
    @Query("DELETE FROM expenses") suspend fun clearExpenses()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM habits") suspend fun clearHabits()
    @Query("DELETE FROM habit_logs") suspend fun clearHabitLogs()
    @Query("DELETE FROM gratitude_entries") suspend fun clearGratitude()
    @Query("DELETE FROM mood_entries") suspend fun clearMoods()
    @Query("DELETE FROM caffeine_doses") suspend fun clearCaffeine()
    @Query("DELETE FROM recurring_cost_history") suspend fun clearRecurringCostHistory()
    @Query("DELETE FROM additional_incomes") suspend fun clearAdditionalIncomes()
}
