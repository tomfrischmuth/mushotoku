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

package com.mushotoku.app.data.backup

import com.mushotoku.app.data.AdditionalIncome
import com.mushotoku.app.data.AppSettings
import com.mushotoku.app.data.CaffeineDose
import com.mushotoku.app.data.Category
import com.mushotoku.app.data.Expense
import com.mushotoku.app.data.GratitudeEntry
import com.mushotoku.app.data.Habit
import com.mushotoku.app.data.HabitLog
import com.mushotoku.app.data.MoodEntry
import com.mushotoku.app.data.Note
import com.mushotoku.app.data.RecurringCostHistory
import com.mushotoku.app.data.Task
import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val schemaVersion: Int,
    val appVersion: String,
    val createdAt: Long,
    val tasks: List<Task> = emptyList(),
    val notes: List<Note> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val settings: AppSettings? = null,
    val habits: List<Habit> = emptyList(),
    val habitLogs: List<HabitLog> = emptyList(),
    val gratitude: List<GratitudeEntry> = emptyList(),
    val moods: List<MoodEntry> = emptyList(),
    val caffeineDoses: List<CaffeineDose> = emptyList(),
    val recurringCostHistory: List<RecurringCostHistory> = emptyList(),
    val additionalIncomes: List<AdditionalIncome> = emptyList(),
)
