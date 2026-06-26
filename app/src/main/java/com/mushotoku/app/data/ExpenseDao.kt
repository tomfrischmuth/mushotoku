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
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE date = :epochDay")
    fun getExpensesForDate(epochDay: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date >= :from AND date <= :to ORDER BY date ASC")
    fun getExpensesForRange(from: Long, to: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date = :epochDay AND category = :category LIMIT 1")
    suspend fun getForDateCategory(epochDay: Long, category: String): Expense?

    @Insert
    suspend fun insert(expense: Expense)

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY date ASC")
    suspend fun getAll(): List<Expense>

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Query("DELETE FROM expenses WHERE category IN (SELECT id FROM categories WHERE isDefault = 0)")
    suspend fun deleteForCustomCategories()

    @Query("DELETE FROM expenses WHERE category = :categoryId")
    suspend fun deleteForCategory(categoryId: String)
}
