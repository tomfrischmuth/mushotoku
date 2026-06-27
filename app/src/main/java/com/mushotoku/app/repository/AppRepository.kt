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

package com.mushotoku.app.repository

import androidx.room.withTransaction
import com.mushotoku.app.data.*
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val db: AppDatabase,
    private val taskDao: TaskDao,
    private val noteDao: NoteDao,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val settingsDao: AppSettingsDao,
    private val habitDao: HabitDao,
    private val recurringCostHistoryDao: RecurringCostHistoryDao,
    private val additionalIncomeDao: AdditionalIncomeDao
) {
    suspend fun <T> inTransaction(block: suspend () -> T): T = db.withTransaction(block)

    fun getTasksForDate(epochDay: Long): Flow<List<Task>> = taskDao.getTasksForDate(epochDay)
    fun getTasksForRange(startDate: Long, endDate: Long): Flow<List<Task>> = taskDao.getTasksForRange(startDate, endDate)
    fun getAllAppointments(): Flow<List<Task>> = taskDao.getAllAppointments()
    suspend fun getTasksForDateOnce(epochDay: Long): List<Task> = taskDao.getTasksForDateOnce(epochDay)
    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()
    fun getDeletedNotes(): Flow<List<Note>> = noteDao.getDeletedNotes()
    suspend fun getAllTasks(): List<Task> = taskDao.getAllTasks()

    suspend fun addTask(task: Task): Long = taskDao.insert(task)
    suspend fun updateTask(task: Task) = taskDao.update(task)
    suspend fun deleteTask(task: Task) = taskDao.delete(task)
    suspend fun deleteAllTasks() = taskDao.deleteAllTasks()
    suspend fun deleteAllAppointments() = taskDao.deleteAllAppointments()
    suspend fun clearLinkedNoteId(noteId: Long) = taskDao.clearLinkedNoteId(noteId)

    suspend fun addNote(note: Note): Long = noteDao.insert(note)
    suspend fun updateNote(note: Note) = noteDao.update(note)
    suspend fun softDeleteNote(note: Note) = noteDao.update(note.copy(isDeleted = true, isPinned = false))
    suspend fun restoreNote(note: Note) = noteDao.update(note.copy(isDeleted = false))
    suspend fun permanentlyDeleteNote(note: Note) = noteDao.delete(note)
    suspend fun permanentlyDeleteAllTrash() = noteDao.deleteAllInTrash()
    suspend fun deleteAllNotes() = noteDao.deleteAll()

    fun getExpensesForDate(epochDay: Long): Flow<List<Expense>> = expenseDao.getExpensesForDate(epochDay)
    fun getExpensesForRange(from: Long, to: Long): Flow<List<Expense>> = expenseDao.getExpensesForRange(from, to)
    suspend fun getExpenseForDateCategory(epochDay: Long, categoryId: String): Expense? =
        expenseDao.getForDateCategory(epochDay, categoryId)
    suspend fun insertExpense(expense: Expense) = expenseDao.insert(expense)
    suspend fun updateExpense(expense: Expense) = expenseDao.update(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.delete(expense)
    suspend fun getAllExpenses(): List<Expense> = expenseDao.getAll()
    suspend fun deleteAllExpenses() = db.withTransaction {
        expenseDao.deleteAll()
        categoryDao.resetAllRecurringCosts()
        recurringCostHistoryDao.deleteAll()
        additionalIncomeDao.deleteAll()
    }

    fun getAdditionalIncomesForDate(epochDay: Long): Flow<List<AdditionalIncome>> =
        additionalIncomeDao.getForDate(epochDay)

    fun getAdditionalIncomesForRange(from: Long, to: Long): Flow<List<AdditionalIncome>> =
        additionalIncomeDao.getForRange(from, to)

    suspend fun insertAdditionalIncome(entry: AdditionalIncome) = additionalIncomeDao.insert(entry)

    suspend fun deleteAdditionalIncome(entry: AdditionalIncome) = additionalIncomeDao.delete(entry)
    suspend fun getAllNotesOnce(): List<Note> = noteDao.getAllOnce()

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun categoriesCount(): Int = categoryDao.count()
    suspend fun insertDefaultCategories(list: List<Category>) = categoryDao.insertAll(list)
    suspend fun insertCategory(category: Category) = categoryDao.insert(category)
    suspend fun updateCategory(category: Category) = categoryDao.update(category)
    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    fun getSettings(): Flow<AppSettings?> = settingsDao.observe()
    suspend fun getSettingsOnce(): AppSettings = settingsDao.getOnce() ?: AppSettings()
    suspend fun initSettings() = settingsDao.insertDefault(AppSettings())
    suspend fun updateSettings(settings: AppSettings) = settingsDao.update(settings)

    fun getAllHabits(): Flow<List<Habit>> = habitDao.observeAll()
    fun getHabitCompletionsForDate(epochDay: Long): Flow<List<String>> = habitDao.observeCompletionsForDate(epochDay)
    suspend fun insertHabit(habit: Habit) = habitDao.insert(habit)
    suspend fun deleteHabit(habit: Habit) = habitDao.delete(habit)
    suspend fun updateHabit(habit: Habit) = habitDao.update(habit)
    suspend fun deleteAllHabits() = db.withTransaction { habitDao.deleteAllHabits(); habitDao.deleteAllHabitLogs() }
    suspend fun logHabitCompletion(habitId: String, epochDay: Long) = habitDao.logCompletion(HabitLog(habitId = habitId, date = epochDay))
    suspend fun removeHabitCompletion(habitId: String, epochDay: Long) = habitDao.removeCompletion(habitId, epochDay)
    fun getAllHabitLogs(): Flow<List<HabitLog>> = habitDao.observeAllLogs()

    fun getAllRecurringCostHistory(): Flow<List<RecurringCostHistory>> = recurringCostHistoryDao.getAll()
    suspend fun getActiveRecurringCostEntry(categoryId: String): RecurringCostHistory? =
        recurringCostHistoryDao.getActiveForCategory(categoryId)
    suspend fun insertRecurringCostEntry(entry: RecurringCostHistory) = recurringCostHistoryDao.insert(entry)
    suspend fun updateRecurringCostEntry(entry: RecurringCostHistory) = recurringCostHistoryDao.update(entry)
    suspend fun deleteRecurringCostEntry(id: Long) = recurringCostHistoryDao.deleteById(id)
    suspend fun deleteRecurringCostHistoryForCategory(categoryId: String) = recurringCostHistoryDao.deleteForCategory(categoryId)
}
