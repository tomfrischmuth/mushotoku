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
import com.mushotoku.app.data.*
import com.mushotoku.app.repository.AppRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class FinanceViewModel(app: Application) : AndroidViewModel(app) {

    private val db   = AppDatabase.getInstance(app)
    private val repo = AppRepository(db, db.taskDao(), db.noteDao(), db.expenseDao(), db.categoryDao(), db.appSettingsDao(), db.habitDao(), db.recurringCostHistoryDao(), db.additionalIncomeDao())

    private val selectedDate = MutableStateFlow(LocalDate.now())
    fun setDate(date: LocalDate) { selectedDate.value = date }

    @OptIn(ExperimentalCoroutinesApi::class)
    val expenses: StateFlow<ImmutableList<Expense>> = selectedDate
        .flatMapLatest { repo.getExpensesForDate(it.toEpochDay()) }
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    val historicalExpenses: StateFlow<ImmutableList<Expense>> = flow {
        val today = LocalDate.now()
        val from  = today.minusMonths(5).withDayOfMonth(1)
        emitAll(repo.getExpensesForRange(from.toEpochDay(), today.toEpochDay()))
    }.map { it.toImmutableList() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    val yearExpenses: StateFlow<ImmutableList<Expense>> = flow {
        val today = LocalDate.now()
        val from  = today.minusYears(1).withDayOfMonth(1)
        emitAll(repo.getExpensesForRange(from.toEpochDay(), today.toEpochDay()))
    }.map { it.toImmutableList() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    val additionalIncomes: StateFlow<ImmutableList<AdditionalIncome>> = selectedDate
        .flatMapLatest { repo.getAdditionalIncomesForDate(it.toEpochDay()) }
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    val historicalAdditionalIncomes: StateFlow<ImmutableList<AdditionalIncome>> = flow {
        val today = LocalDate.now()
        val from  = today.minusMonths(5).withDayOfMonth(1)
        emitAll(repo.getAdditionalIncomesForRange(from.toEpochDay(), today.toEpochDay()))
    }.map { it.toImmutableList() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    val categories: StateFlow<ImmutableList<Category>> = repo.getAllCategories()
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    val recurringCostHistory: StateFlow<ImmutableList<RecurringCostHistory>> = repo.getAllRecurringCostHistory()
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    fun getAllExpensesForExport(callback: (List<Expense>) -> Unit) = viewModelScope.launch {
        callback(repo.getAllExpenses())
    }

    fun deleteAllExpenses() = viewModelScope.launch {
        repo.inTransaction {
            repo.deleteAllExpenses()
            repo.updateSettings(repo.getSettingsOnce().copy(salary = 0.0))
        }
    }

    fun addAdditionalIncome(label: String, amount: Double) = viewModelScope.launch {
        if (label.isBlank() || amount <= 0.0) return@launch
        repo.insertAdditionalIncome(
            AdditionalIncome(date = selectedDate.value.toEpochDay(), label = label.trim(), amount = amount)
        )
    }

    fun deleteAdditionalIncome(income: AdditionalIncome) = viewModelScope.launch {
        repo.deleteAdditionalIncome(income)
    }

    fun addExpense(categoryId: String, date: LocalDate) = viewModelScope.launch {
        val epochDay = date.toEpochDay()
        val existing = repo.getExpenseForDateCategory(epochDay, categoryId)
        if (existing != null) {
            repo.updateExpense(existing.copy(amount = existing.amount + 1.0))
        } else {
            repo.insertExpense(Expense(date = epochDay, category = categoryId, amount = 1.0))
        }
    }

    fun setExpenseAmount(categoryId: String, amount: Double, date: LocalDate) = viewModelScope.launch {
        val epochDay = date.toEpochDay()
        val existing = repo.getExpenseForDateCategory(epochDay, categoryId)
        if (amount <= 0.0) {
            existing?.let { repo.deleteExpense(it) }
        } else if (existing != null) {
            repo.updateExpense(existing.copy(amount = amount))
        } else {
            repo.insertExpense(Expense(date = epochDay, category = categoryId, amount = amount))
        }
    }

    fun removeExpense(categoryId: String, date: LocalDate) = viewModelScope.launch {
        val epochDay = date.toEpochDay()
        val existing = repo.getExpenseForDateCategory(epochDay, categoryId)
        if (existing != null) {
            if (existing.amount <= 0.5) repo.deleteExpense(existing)
            else repo.updateExpense(existing.copy(amount = existing.amount - 0.5))
        }
    }

    fun setCategoryEnabled(category: Category, enabled: Boolean) = viewModelScope.launch {
        repo.updateCategory(category.copy(isEnabled = enabled))
    }

    fun setCategoryRecurringCost(category: Category, cost: Double) = viewModelScope.launch {
        val now = YearMonth.now()
        val currentMonthStr  = "%04d-%02d".format(now.year, now.monthValue)
        val previousMonthStr = now.minusMonths(1).let { "%04d-%02d".format(it.year, it.monthValue) }
        val active = repo.getActiveRecurringCostEntry(category.id)

        repo.inTransaction {
            when {
                active != null && cost > 0.0 -> {
                    if (active.startMonth == currentMonthStr) {
                        repo.updateRecurringCostEntry(active.copy(amount = cost, categoryName = category.name))
                    } else {
                        repo.updateRecurringCostEntry(active.copy(endMonth = previousMonthStr))
                        repo.insertRecurringCostEntry(RecurringCostHistory(
                            categoryId = category.id, categoryName = category.name,
                            amount = cost, startMonth = currentMonthStr, endMonth = null
                        ))
                    }
                }
                active != null && cost <= 0.0 -> {
                    if (active.startMonth == currentMonthStr) {
                        repo.deleteRecurringCostEntry(active.id)
                    } else {
                        repo.updateRecurringCostEntry(active.copy(endMonth = previousMonthStr))
                    }
                }
                active == null && cost > 0.0 -> {
                    repo.insertRecurringCostEntry(RecurringCostHistory(
                        categoryId = category.id, categoryName = category.name,
                        amount = cost, startMonth = currentMonthStr, endMonth = null
                    ))
                }
            }
            repo.updateCategory(category.copy(recurringCost = cost))
        }
    }

    fun addCustomCategory(name: String, group: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        val maxOrder = categories.value.maxOfOrNull { it.sortOrder } ?: 23
        repo.insertCategory(
            Category(
                id        = "custom_${System.currentTimeMillis()}",
                name      = name.trim(),
                group     = group,
                isDefault = false,
                sortOrder = maxOrder + 1
            )
        )
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        repo.inTransaction {
            if (category.recurringCost > 0.0) {
                val now = YearMonth.now()
                val currentMonthStr  = "%04d-%02d".format(now.year, now.monthValue)
                val previousMonthStr = now.minusMonths(1).let { "%04d-%02d".format(it.year, it.monthValue) }
                val active = repo.getActiveRecurringCostEntry(category.id)
                if (active != null) {
                    if (active.startMonth == currentMonthStr) repo.deleteRecurringCostEntry(active.id)
                    else repo.updateRecurringCostEntry(active.copy(endMonth = previousMonthStr))
                }
            }
            repo.deleteCategory(category)
        }
    }
}
