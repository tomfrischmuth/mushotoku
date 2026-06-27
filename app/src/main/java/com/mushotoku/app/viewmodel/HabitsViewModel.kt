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
import com.mushotoku.app.data.isScheduledFor
import com.mushotoku.app.repository.AppRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HabitsViewModel(app: Application) : AndroidViewModel(app) {

    private val db   = AppDatabase.getInstance(app)
    private val repo = AppRepository(db, db.taskDao(), db.noteDao(), db.expenseDao(), db.categoryDao(), db.appSettingsDao(), db.habitDao(), db.recurringCostHistoryDao(), db.additionalIncomeDao())

    private val selectedDate = MutableStateFlow(LocalDate.now())
    fun setDate(date: LocalDate) { selectedDate.value = date }

    val habits: StateFlow<ImmutableList<Habit>> = repo.getAllHabits()
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    val scheduledHabits: StateFlow<ImmutableList<Habit>> = combine(selectedDate, habits) { date, habitList ->
        habitList.filter { it.isScheduledFor(date.toEpochDay()) }.toImmutableList()
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    val habitCompletions: StateFlow<ImmutableSet<String>> = selectedDate
        .flatMapLatest { repo.getHabitCompletionsForDate(it.toEpochDay()) }
        .map { it.toImmutableSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentSetOf())

    val allHabitLogs: StateFlow<ImmutableList<HabitLog>> = repo.getAllHabitLogs()
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    val habitStreaks: StateFlow<ImmutableMap<String, Int>> = combine(habits, allHabitLogs) { habitList, logs ->
        val today = LocalDate.now().toEpochDay()
        val datesByHabit = HashMap<String, HashSet<Long>>()
        for (log in logs) {
            datesByHabit.getOrPut(log.habitId) { HashSet() }.add(log.date)
        }
        habitList
            .filter { it.recurrence == Recurrence.DAILY }
            .associate { habit ->
                val done = datesByHabit[habit.id] ?: emptySet<Long>()
                var streak = 0
                if (today in done) {
                    var day = today
                    while (day in done) { streak++; day-- }
                }
                habit.id to streak
            }
            .toImmutableMap()
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentMapOf())

    fun toggleHabit(habit: Habit, date: LocalDate) = viewModelScope.launch {
        val epochDay = date.toEpochDay()
        if (habit.id in habitCompletions.value) {
            repo.removeHabitCompletion(habit.id, epochDay)
        } else {
            repo.logHabitCompletion(habit.id, epochDay)
        }
    }

    fun addHabit(name: String, recurrence: String = "DAILY") = viewModelScope.launch {
        if (name.isBlank()) return@launch
        val maxOrder = habits.value.maxOfOrNull { it.sortOrder } ?: -1
        repo.insertHabit(Habit(
            id           = "habit_${System.currentTimeMillis()}",
            name         = name.trim(),
            recurrence   = recurrence,
            createdAtDay = selectedDate.value.toEpochDay(),
            sortOrder    = maxOrder + 1
        ))
    }

    fun deleteHabit(habit: Habit) = viewModelScope.launch { repo.deleteHabit(habit) }

    fun updateHabit(habit: Habit) = viewModelScope.launch { repo.updateHabit(habit) }

    fun deleteAllHabits() = viewModelScope.launch { repo.deleteAllHabits() }
}
