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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class TasksViewModel(app: Application) : AndroidViewModel(app) {

    private val db   = AppDatabase.getInstance(app)
    private val repo = AppRepository(db, db.taskDao(), db.noteDao(), db.expenseDao(), db.categoryDao(), db.appSettingsDao(), db.habitDao(), db.recurringCostHistoryDao(), db.additionalIncomeDao())

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val calendarMonth = MutableStateFlow(YearMonth.now())

    fun setDate(date: LocalDate) { selectedDate.value = date }
    fun setMonth(month: YearMonth) { calendarMonth.value = month }

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<ImmutableList<Task>> = selectedDate
        .flatMapLatest { repo.getTasksForDate(it.toEpochDay()) }
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    val allAppointments: StateFlow<ImmutableList<Task>> = repo.getAllAppointments()
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    val appointmentsForMonth: StateFlow<ImmutableList<Task>> = calendarMonth
        .flatMapLatest { month ->
            repo.getTasksForRange(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())
        }
        .map { list -> list.filter { it.isAppointment }.toImmutableList() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    fun addTask(title: String, isAppointment: Boolean = false, time: String = "", linkedNoteId: Long? = null) = viewModelScope.launch {
        if (title.isNotBlank()) repo.addTask(
            Task(
                title = title.trim(),
                date = selectedDate.value.toEpochDay(),
                isAppointment = isAppointment,
                time = time,
                linkedNoteId = linkedNoteId
            )
        )
    }

    fun addAppointmentForDate(title: String, time: String, date: LocalDate) = viewModelScope.launch {
        if (title.isNotBlank()) repo.addTask(
            Task(title = title.trim(), date = date.toEpochDay(), isAppointment = true, time = time)
        )
    }

    fun toggleDone(task: Task) = viewModelScope.launch {
        repo.updateTask(task.copy(isDone = !task.isDone))
    }

    fun cycleStatus(task: Task) = viewModelScope.launch {
        val next = when (task.status) {
            TaskStatus.RED    -> TaskStatus.YELLOW
            TaskStatus.YELLOW -> TaskStatus.GREEN
            TaskStatus.GREEN  -> TaskStatus.RED
        }
        repo.updateTask(task.copy(status = next))
    }

    fun moveToTomorrow(task: Task) = viewModelScope.launch {
        val tomorrow = LocalDate.ofEpochDay(task.date).plusDays(1)
        repo.updateTask(task.copy(date = tomorrow.toEpochDay()))
    }

    fun moveToDate(task: Task, date: LocalDate, time: String = task.time) = viewModelScope.launch {
        repo.updateTask(task.copy(date = date.toEpochDay(), time = time))
    }

    fun deleteTask(task: Task) = viewModelScope.launch { repo.deleteTask(task) }

    fun updateTaskTitle(task: Task, newTitle: String) = viewModelScope.launch {
        if (newTitle.isNotBlank()) repo.updateTask(task.copy(title = newTitle.trim()))
    }

    fun reorderTasks(ordered: List<Task>) = viewModelScope.launch {
        repo.inTransaction {
            ordered.forEachIndexed { i, task ->
                if (task.sortOrder != i) repo.updateTask(task.copy(sortOrder = i))
            }
        }
    }

    fun deleteAllTasks() = viewModelScope.launch { repo.deleteAllTasks() }
    fun deleteAllAppointments() = viewModelScope.launch { repo.deleteAllAppointments() }

    fun getAllTasksForExport(callback: (List<Task>) -> Unit) = viewModelScope.launch {
        callback(repo.getAllTasks())
    }
}
